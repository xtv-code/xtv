package com.xtv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Default Spring scheduling uses a single thread. Transcode jobs would otherwise
 * block payment reconcile for minutes at a time.
 */
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {
  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(4);
    scheduler.setThreadNamePrefix("xtv-job-");
    scheduler.initialize();
    taskRegistrar.setTaskScheduler(scheduler);
  }
}
