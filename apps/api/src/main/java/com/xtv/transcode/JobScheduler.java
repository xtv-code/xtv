package com.xtv.transcode;

import com.xtv.billing.BillingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "xtv.jobs-enabled", havingValue = "true", matchIfMissing = true)
public class JobScheduler {
  private final TranscodeService transcode;
  private final BillingService billing;

  public JobScheduler(TranscodeService transcode, BillingService billing) {
    this.transcode = transcode;
    this.billing = billing;
  }

  @Scheduled(fixedDelay = 5000)
  public void transcode() {
    transcode.poll();
  }

  @Scheduled(fixedDelay = 8000)
  public void payments() {
    billing.reconcile();
  }
}
