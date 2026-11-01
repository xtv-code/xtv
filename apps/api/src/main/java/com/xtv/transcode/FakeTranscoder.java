package com.xtv.transcode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
@Primary
public class FakeTranscoder implements Transcoder {
  private static final byte[] JPEG = Base64.getDecoder().decode(
      "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAkGBxISEhIQEhIVFhUVFRUVFRUVFRUVFRUWFxUVFRUYHSggGBolGxUVITEhJSkrLi4uFx8zODMtNygtLisBCgoKDg0OGhAQGy0lHyUtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLf/AABEIAAEAAQMBIgACEQEDEQH/xAAbAAACAwEBAQAAAAAAAAAAAAADBAECBQYAB//EAB4QAAIBAwUAAAAAAAAAAAAAAAACAQMEERIhIjFR/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwD6qAAAH//Z");

  @Override
  public TranscodeOutput transcode(Path source, Path outputDir) throws Exception {
    Files.createDirectories(outputDir);
    Files.writeString(outputDir.resolve("360p.m3u8"), "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-ENDLIST\n");
    Files.writeString(outputDir.resolve("master.m3u8"),
        "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360\n360p.m3u8\n");
    Path cover = outputDir.resolve("cover.jpg");
    Files.write(cover, JPEG);
    return new TranscodeOutput(1500, cover, List.of(new Rendition(360, 800_000, "360p.m3u8")));
  }
}
