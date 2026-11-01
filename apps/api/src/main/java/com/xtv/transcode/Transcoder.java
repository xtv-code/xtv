package com.xtv.transcode;

import java.nio.file.Path;
import java.util.List;

public interface Transcoder {
  TranscodeOutput transcode(Path source, Path outputDir) throws Exception;

  record Rendition(int height, int bandwidth, String playlistName) {}

  record TranscodeOutput(long durationMs, Path cover, List<Rendition> renditions) {}
}
