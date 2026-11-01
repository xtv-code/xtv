package com.xtv.transcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class FfmpegTranscoder implements Transcoder {
  private final ObjectMapper json = new ObjectMapper();

  @Override
  public TranscodeOutput transcode(Path source, Path outputDir) throws Exception {
    Files.createDirectories(outputDir);
    Probe probe = probe(source);
    int sourceHeight = probe.height() <= 0 ? 1080 : probe.height();
    List<Rendition> renditions = new ArrayList<>();
    renditions.add(encode(source, outputDir, 360, 800_000, probe.hasAudio()));
    if (sourceHeight >= 720) {
      renditions.add(encode(source, outputDir, 720, 2_500_000, probe.hasAudio()));
    }
    if (sourceHeight >= 1080) {
      renditions.add(encode(source, outputDir, 1080, 5_000_000, probe.hasAudio()));
    }
    StringBuilder master = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n");
    for (Rendition rendition : renditions) {
      int width = rendition.height() * 16 / 9;
      master.append("#EXT-X-STREAM-INF:BANDWIDTH=").append(rendition.bandwidth())
          .append(",RESOLUTION=").append(width).append('x').append(rendition.height()).append('\n')
          .append(rendition.playlistName()).append('\n');
    }
    Files.writeString(outputDir.resolve("master.m3u8"), master.toString());
    Path cover = outputDir.resolve("cover.jpg");
    run(List.of("ffmpeg", "-y", "-ss", "1", "-i", source.toString(), "-frames:v", "1", cover.toString()), 60);
    return new TranscodeOutput(probe.durationMs(), Files.exists(cover) ? cover : null, renditions);
  }

  private Rendition encode(Path source, Path outputDir, int height, int bandwidth, boolean audio) throws Exception {
    String name = height + "p";
    Path playlist = outputDir.resolve(name + ".m3u8");
    Path segment = outputDir.resolve(name + "_%03d.ts");
    List<String> cmd = new ArrayList<>();
    cmd.add("ffmpeg");
    cmd.add("-y");
    cmd.add("-i");
    cmd.add(source.toString());
    cmd.add("-vf");
    cmd.add("scale=-2:" + height);
    cmd.add("-c:v");
    cmd.add("libx264");
    cmd.add("-preset");
    cmd.add("veryfast");
    cmd.add("-b:v");
    cmd.add(bandwidth + "");
    if (audio) {
      cmd.add("-c:a");
      cmd.add("aac");
      cmd.add("-b:a");
      cmd.add("128k");
    } else {
      cmd.add("-an");
    }
    cmd.add("-hls_time");
    cmd.add("6");
    cmd.add("-hls_playlist_type");
    cmd.add("vod");
    cmd.add("-hls_segment_filename");
    cmd.add(segment.toString());
    cmd.add(playlist.toString());
    run(cmd, 60 * 30);
    return new Rendition(height, bandwidth, name + ".m3u8");
  }

  private Probe probe(Path source) throws Exception {
    String raw = run(List.of("ffprobe", "-v", "error", "-show_entries", "format=duration:stream=codec_type,height",
        "-of", "json", source.toString()), 30);
    JsonNode root = json.readTree(raw);
    long durationMs = 0;
    if (root.path("format").has("duration")) {
      durationMs = Math.round(root.path("format").path("duration").asDouble() * 1000);
    }
    int height = 0;
    boolean audio = false;
    for (JsonNode stream : root.path("streams")) {
      if ("video".equals(stream.path("codec_type").asText()) && stream.has("height")) {
        height = stream.path("height").asInt();
      }
      if ("audio".equals(stream.path("codec_type").asText())) {
        audio = true;
      }
    }
    return new Probe(durationMs, height, audio);
  }

  private String run(List<String> command, int timeoutSeconds) throws Exception {
    Path log = Files.createTempFile("xtv-ffmpeg", ".log");
    Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException("转码超时");
    }
    String text = Files.readString(log);
    Files.deleteIfExists(log);
    if (process.exitValue() != 0) {
      throw new IllegalStateException(text.length() > 1500 ? text.substring(text.length() - 1500) : text);
    }
    return text;
  }

  private record Probe(long durationMs, int height, boolean hasAudio) {}
}
