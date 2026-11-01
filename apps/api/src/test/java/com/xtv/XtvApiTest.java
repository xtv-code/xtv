package com.xtv;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xtv.transcode.TranscodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XtvApiTest {
  @Autowired
  MockMvc mvc;
  @Autowired
  TranscodeService transcode;

  @Test
  void uploadPlayCatalogAdsAndUsdt() throws Exception {
    String admin = token("admin", "admin123");

    mvc.perform(post("/api/v1/admin/categories").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"剧情\",\"showInNav\":true,\"showOnHome\":true,\"sortOrder\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("剧情"));

    mvc.perform(post("/api/v1/admin/tags").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"热门\",\"showInNav\":true,\"sortOrder\":1}"))
        .andExpect(status().isOk());

    MvcResult created = mvc.perform(post("/api/v1/admin/videos").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"测试短片\"}"))
        .andExpect(status().isOk())
        .andReturn();
    String videoId = json(created, "id");

    mvc.perform(multipart("/api/v1/admin/videos/" + videoId + "/source").file(
            new MockMultipartFile("file", "clip.mp4", "video/mp4", "fake".getBytes()))
            .header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("PENDING"));

    transcode.poll();

    mvc.perform(put("/api/v1/admin/videos/" + videoId).header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"categoryIds\":[1],\"tagIds\":[1],\"accessType\":\"FREE\",\"featured\":true}"))
        .andExpect(status().isOk());

    mvc.perform(put("/api/v1/admin/videos/" + videoId + "/publish").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    mvc.perform(get("/api/v1/home"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.featured[0].title").value("测试短片"));

    mvc.perform(get("/api/v1/nav"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[0].name").value("剧情"));

    MvcResult play = mvc.perform(get("/api/v1/videos/" + videoId + "/play"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(true))
        .andExpect(jsonPath("$.playUrl").exists())
        .andReturn();
    String playUrl = play.getResponse().getContentAsString();
    int urlStart = playUrl.indexOf("\"playUrl\":\"") + 11;
    String signed = playUrl.substring(urlStart, playUrl.indexOf('"', urlStart)).replace("\\u003d", "=");
    String path = signed.substring(signed.indexOf("/media/"));
    org.junit.jupiter.api.Assertions.assertTrue(path.startsWith("/media/v/"), path);
    mvc.perform(get("/media/" + videoId + "/master.m3u8")).andExpect(status().isNotFound());
    mvc.perform(get(path)).andExpect(status().isOk());

    mvc.perform(multipart("/api/v1/admin/ads")
            .file(new MockMultipartFile("file", "ad.jpg", "image/jpeg", new byte[] {1, 2, 3}))
            .param("slot", "TOP")
            .param("targetUrl", "https://example.com")
            .header("Authorization", admin))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/ads").param("slot", "TOP"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targetUrl").value("https://example.com"));

    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"viewer\",\"password\":\"secret1\"}"))
        .andExpect(status().isOk());
    String viewer = token("viewer", "secret1");

    mvc.perform(put("/api/v1/admin/videos/" + videoId).header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"accessType\":\"PPV\",\"priceUsdt\":2}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/videos/" + videoId + "/play").header("Authorization", viewer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(false));
    mvc.perform(get("/api/v1/videos/" + videoId + "/play").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(false))
        .andExpect(jsonPath("$.playUrl").value(org.hamcrest.Matchers.nullValue()));

    MvcResult order = mvc.perform(post("/api/v1/orders").header("Authorization", viewer)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"PPV\",\"videoId\":" + videoId + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("PENDING"))
        .andReturn();
    String orderId = json(order, "id");
    mvc.perform(post("/api/v1/admin/orders/" + orderId + "/confirm").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("PAID"));
    mvc.perform(get("/api/v1/videos/" + videoId + "/play").header("Authorization", viewer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(true));

    mvc.perform(post("/api/v1/admin/plans").header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"月度\",\"durationDays\":30,\"priceUsdt\":10,\"enabled\":true}"))
        .andExpect(status().isOk());
    MvcResult memberOrder = mvc.perform(post("/api/v1/orders").header("Authorization", viewer)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"MEMBERSHIP\",\"planId\":1}"))
        .andExpect(status().isOk())
        .andReturn();
    mvc.perform(post("/api/v1/admin/orders/" + json(memberOrder, "id") + "/confirm").header("Authorization", admin))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/me").header("Authorization", viewer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memberUntil").exists());
  }

  private String token(String username, String password) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn();
    String body = result.getResponse().getContentAsString();
    int start = body.indexOf("\"token\":\"") + 9;
    return "Bearer " + body.substring(start, body.indexOf('"', start));
  }

  private String json(MvcResult result, String field) throws Exception {
    String body = result.getResponse().getContentAsString();
    String key = "\"" + field + "\":";
    int start = body.indexOf(key) + key.length();
    int end = start;
    while (end < body.length() && Character.isDigit(body.charAt(end))) {
      end++;
    }
    return body.substring(start, end);
  }
}
