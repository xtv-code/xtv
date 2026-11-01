package com.xtv.transcode;

import com.xtv.domain.TranscodeJob;
import com.xtv.domain.Video;
import com.xtv.domain.VideoAsset;
import com.xtv.domain.VideoRendition;
import com.xtv.domain.Vocab;
import com.xtv.repo.TranscodeJobRepository;
import com.xtv.repo.VideoAssetRepository;
import com.xtv.repo.VideoRenditionRepository;
import com.xtv.repo.VideoRepository;
import com.xtv.service.MediaStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranscodeTx {
  private final TranscodeJobRepository jobs;
  private final VideoRepository videos;
  private final VideoAssetRepository assets;
  private final VideoRenditionRepository renditions;
  private final MediaStore media;

  public TranscodeTx(TranscodeJobRepository jobs, VideoRepository videos, VideoAssetRepository assets,
                     VideoRenditionRepository renditions, MediaStore media) {
    this.jobs = jobs;
    this.videos = videos;
    this.assets = assets;
    this.renditions = renditions;
    this.media = media;
  }

  @Transactional
  public boolean claim(Long id) {
    return jobs.claim(id, Instant.now()) == 1;
  }

  @Transactional
  public Prep prepare(Long jobId) throws Exception {
    TranscodeJob job = jobs.findById(jobId).orElseThrow();
    Video video = videos.findById(job.getVideoId()).orElseThrow();
    VideoAsset asset = assets.findByVideoId(video.getId()).orElseThrow();
    video.setStatus(Vocab.VideoStatus.TRANSCODING);
    job.setProgress(10);
    job.setUpdatedAt(Instant.now());
    Path output = media.hlsDir(video.getId());
    if (Files.exists(output)) {
      try (var walk = Files.walk(output)) {
        walk.sorted(Comparator.reverseOrder()).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (Exception ignored) {
            // 清理旧分片失败时由后续写入覆盖
          }
        });
      }
    }
    Files.createDirectories(output);
    Path source = media.materializeOriginal(asset);
    return new Prep(source, output);
  }

  @Transactional
  public void finish(Long jobId, Transcoder.TranscodeOutput output) throws Exception {
    TranscodeJob job = jobs.findById(jobId).orElseThrow();
    Video video = videos.findById(job.getVideoId()).orElseThrow();
    video.setDurationMs(output.durationMs());
    if (output.cover() != null && Files.exists(output.cover())) {
      Path cover = media.publicFile("covers/" + video.getId() + ".jpg");
      Files.createDirectories(cover.getParent());
      Files.copy(output.cover(), cover, StandardCopyOption.REPLACE_EXISTING);
      video.setCoverKey("covers/" + video.getId() + ".jpg");
    }
    renditions.deleteByVideoId(video.getId());
    for (Transcoder.Rendition rendition : output.renditions()) {
      VideoRendition row = new VideoRendition();
      row.setVideoId(video.getId());
      row.setHeight(rendition.height());
      row.setBandwidth(rendition.bandwidth());
      row.setPlaylistKey(video.getId() + "/" + rendition.playlistName());
      renditions.save(row);
    }
    media.uploadPublicTree(video.getId());
    video.setStatus(video.getPublishedAt() != null ? Vocab.VideoStatus.PUBLISHED : Vocab.VideoStatus.READY);
    job.setState(Vocab.JobState.SUCCEEDED);
    job.setProgress(100);
    job.setErrorMessage(null);
    job.setUpdatedAt(Instant.now());
  }

  @Transactional
  public void fail(Long jobId, String message) {
    TranscodeJob job = jobs.findById(jobId).orElseThrow();
    Video video = videos.findById(job.getVideoId()).orElseThrow();
    String text = message == null ? "转码失败" : message;
    if (text.length() > 1800) {
      text = text.substring(0, 1800);
    }
    job.setErrorMessage(text);
    job.setRetryCount(job.getRetryCount() + 1);
    job.setUpdatedAt(Instant.now());
    if (job.getRetryCount() < 3) {
      job.setState(Vocab.JobState.PENDING);
      video.setStatus(Vocab.VideoStatus.UPLOADED);
    } else {
      job.setState(Vocab.JobState.FAILED);
      video.setStatus(Vocab.VideoStatus.FAILED);
    }
  }

  public record Prep(Path source, Path output) {}
}
