package com.xtv.service;

import com.xtv.domain.Video;
import com.xtv.dto.Models;
import com.xtv.repo.VideoRepository;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImportService {
  private static final int MAX_IMAGE_BYTES = 8_000_000;
  private static final Duration TIMEOUT = Duration.ofSeconds(12);

  private final HttpClient http = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NEVER)
      .connectTimeout(TIMEOUT)
      .build();
  private final VideoRepository videoRepo;
  private final MediaStore media;
  private final CatalogService catalog;
  private final TaxonomyService taxonomy;

  public ImportService(VideoRepository videoRepo, MediaStore media, CatalogService catalog, TaxonomyService taxonomy) {
    this.videoRepo = videoRepo;
    this.media = media;
    this.catalog = catalog;
    this.taxonomy = taxonomy;
  }

  @Transactional
  public Models.VideoDetail saveVideoCover(Long videoId, String url) {
    if (videoRepo.findById(videoId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在");
    }
    String key = "covers/" + videoId + ".jpg";
    writeJpeg(key, url);
    Video video = videoRepo.findById(videoId).orElseThrow();
    video.setCoverKey(key);
    return catalog.detail(videoId);
  }

  public Models.SeriesCard saveSeriesCover(Long seriesId, String url) {
    String key = "series/" + seriesId + ".jpg";
    writeJpeg(key, url);
    return taxonomy.setCover(seriesId, key);
  }

  @Transactional
  public Models.VideoDetail saveVideoCoverFile(Long videoId, MultipartFile file) {
    if (videoRepo.findById(videoId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在");
    }
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择图片");
    }
    if (file.getSize() > MAX_IMAGE_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片太大");
    }
    try {
      writeJpeg("covers/" + videoId + ".jpg", file.getBytes());
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面没有保存下来");
    }
    Video video = videoRepo.findById(videoId).orElseThrow();
    video.setCoverKey("covers/" + videoId + ".jpg");
    return catalog.detail(videoId);
  }

  private void writeJpeg(String key, String url) {
    Fetched image = fetch(url);
    writeJpeg(key, image.body());
  }

  private void writeJpeg(String key, byte[] body) {
    BufferedImage source;
    try {
      source = ImageIO.read(new ByteArrayInputStream(body));
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面没有保存下来");
    }
    if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0
        || source.getWidth() > 8000 || source.getHeight() > 8000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这张图暂时存不了");
    }
    BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = rgb.createGraphics();
    graphics.setColor(Color.BLACK);
    graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
    graphics.drawImage(source, 0, 0, null);
    graphics.dispose();
    ByteArrayOutputStream jpg = new ByteArrayOutputStream();
    try {
      if (!ImageIO.write(rgb, "jpg", jpg)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这张图暂时存不了");
      }
      media.writePublic(key, new ByteArrayInputStream(jpg.toByteArray()));
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面没有保存下来");
    }
  }

  private Fetched fetch(String rawUrl) {
    URI current = parse(rawUrl);
    try {
      for (int hop = 0; hop < 5; hop++) {
        assertPublic(current);
        HttpRequest request = HttpRequest.newBuilder(current)
            .timeout(TIMEOUT)
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "image/*,*/*")
            .GET()
            .build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        int status = response.statusCode();
        if (status >= 300 && status < 400) {
          String location = response.headers().firstValue("location").orElse("");
          response.body().close();
          if (location.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面没有保存下来");
          }
          current = current.resolve(location);
          continue;
        }
        try (InputStream in = response.body()) {
          if (status < 200 || status >= 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面没有保存下来");
          }
          return new Fetched(readLimited(in, MAX_IMAGE_BYTES));
        }
      }
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面没有保存下来");
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面没有保存下来");
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面没有保存下来");
  }

  private static URI parse(String rawUrl) {
    String text = rawUrl == null ? "" : rawUrl.trim();
    if (text.isBlank() || text.length() > 1000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入图片链接");
    }
    try {
      URI uri = new URI(text);
      if (uri.getScheme() == null || uri.getHost() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入图片链接");
      }
      return uri;
    } catch (URISyntaxException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入图片链接");
    }
  }

  private static void assertPublic(URI uri) {
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!scheme.equals("http") && !scheme.equals("https")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入图片链接");
    }
    if (uri.getUserInfo() != null || uri.getHost() == null || uri.getHost().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这个地址不能打开");
    }
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local") || host.equals("0.0.0.0")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这个地址不能打开");
    }
    try {
      for (InetAddress address : InetAddress.getAllByName(host)) {
        if (blocked(address)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这个地址不能打开");
        }
      }
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封面没有保存下来");
    }
  }

  private static boolean blocked(InetAddress address) {
    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
      return true;
    }
    byte[] bytes = address.getAddress();
    if (bytes.length == 4) {
      int a = bytes[0] & 0xff;
      int b = bytes[1] & 0xff;
      if (a == 0 || a == 127 || a == 10) return true;
      if (a == 100 && b >= 64 && b <= 127) return true;
      if (a == 169 && b == 254) return true;
      if (a == 172 && b >= 16 && b <= 31) return true;
      if (a == 192 && b == 168) return true;
    }
    return false;
  }

  private static byte[] readLimited(InputStream in, int max) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int total = 0;
    int n;
    while ((n = in.read(buf)) >= 0) {
      total += n;
      if (total > max) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片太大");
      }
      out.write(buf, 0, n);
    }
    return out.toByteArray();
  }

  private record Fetched(byte[] body) {}
}
