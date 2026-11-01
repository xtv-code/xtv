package com.xtv.transcode;

import com.xtv.domain.TranscodeJob;
import com.xtv.domain.Vocab;
import com.xtv.repo.TranscodeJobRepository;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Service;

@Service
public class TranscodeService {
  private final TranscodeJobRepository jobs;
  private final TranscodeTx tx;
  private final Transcoder transcoder;
  private final Semaphore running = new Semaphore(1);

  public TranscodeService(TranscodeJobRepository jobs, TranscodeTx tx, Transcoder transcoder) {
    this.jobs = jobs;
    this.tx = tx;
    this.transcoder = transcoder;
  }

  public void poll() {
    if (!running.tryAcquire()) {
      return;
    }
    try {
      for (TranscodeJob job : jobs.findTop5ByStateOrderByCreatedAtAsc(Vocab.JobState.PENDING)) {
        if (!tx.claim(job.getId())) {
          continue;
        }
        try {
          TranscodeTx.Prep prep = tx.prepare(job.getId());
          Transcoder.TranscodeOutput output = transcoder.transcode(prep.source(), prep.output());
          tx.finish(job.getId(), output);
        } catch (Exception ex) {
          tx.fail(job.getId(), ex.getMessage());
        }
        return;
      }
    } finally {
      running.release();
    }
  }
}
