package com.xtv.web;

import com.xtv.service.MediaStore;
import com.xtv.service.MediaTokenService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

@RestController
public class MediaController {
  private final MediaTokenService tokens;
  private final MediaStore media;

  public MediaController(MediaTokenService tokens, MediaStore media) {
    this.tokens = tokens;
    this.media = media;
  }

  @GetMapping("/media/v/{exp}/{sig}/{videoId}/**")
  public ResponseEntity<InputStreamResource> signed(
      @PathVariable long exp,
      @PathVariable String sig,
      @PathVariable long videoId,
      HttpServletRequest request) throws Exception {
    tokens.verify(videoId, exp, sig);
    String full = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    String prefix = "/media/v/" + exp + "/" + sig + "/" + videoId + "/";
    if (full == null || !full.startsWith(prefix)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    String rest = full.substring(prefix.length());
    if (rest.isBlank() || rest.contains("..")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    Path file = media.publicFile(videoId + "/" + rest);
    if (!Files.isRegularFile(file)) {
      if (media.objectMode() && media.objectConfigured()) {
        try (InputStream in = media.openObject(videoId + "/" + rest)) {
          byte[] bytes = in.readAllBytes();
          return ResponseEntity.ok()
              .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
              .contentType(contentType(rest))
              .contentLength(bytes.length)
              .body(new InputStreamResource(new java.io.ByteArrayInputStream(bytes)));
        } catch (Exception ex) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
      }
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    InputStream in = Files.newInputStream(file);
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
        .contentType(contentType(rest))
        .contentLength(Files.size(file))
        .body(new InputStreamResource(in));
  }

  private static MediaType contentType(String name) {
    String lower = name.toLowerCase();
    if (lower.endsWith(".m3u8")) {
      return MediaType.parseMediaType("application/vnd.apple.mpegurl");
    }
    if (lower.endsWith(".ts")) {
      return MediaType.parseMediaType("video/mp2t");
    }
    if (lower.endsWith(".m4s")) {
      return MediaType.parseMediaType("video/iso.segment");
    }
    if (lower.endsWith(".mp4")) {
      return MediaType.parseMediaType("video/mp4");
    }
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return MediaType.IMAGE_JPEG;
    }
    if (lower.endsWith(".png")) {
      return MediaType.IMAGE_PNG;
    }
    return MediaType.APPLICATION_OCTET_STREAM;
  }
}