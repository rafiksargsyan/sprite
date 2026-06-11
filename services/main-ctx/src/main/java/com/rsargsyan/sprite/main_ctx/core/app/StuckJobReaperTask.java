package com.rsargsyan.sprite.main_ctx.core.app;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Profile("web")
@Slf4j
@Configuration
public class StuckJobReaperTask {

  @SuppressWarnings(value = "unused")
  @Bean
  public RecurringTask<Void> stuckJobReaper(ThumbnailsGenerationJobService jobService) {
    return Tasks.recurring("stuck-job-reaper", Schedules.fixedDelay(Duration.ofSeconds(60)))
        .execute((instance, ctx) -> {
          log.info("Running stuck job reaper");
          jobService.retryStuckJobs();
        });
  }
}
