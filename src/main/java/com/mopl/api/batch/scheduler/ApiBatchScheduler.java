package com.mopl.api.batch.scheduler;


import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job getSportApiJob;
    private final Job getMovieApiJob;
    private final Job getTvSeriesApiJob;

    @Scheduled(cron = "${batch.schedule.sport}")
    public void runSportJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("date", LocalDate.now()
                                        .toString())
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();

        jobLauncher.run(getSportApiJob, jobParameters);
    }

    @Scheduled(cron = "${batch.schedule.movie}")
    public void runMovieJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();
        jobLauncher.run(getMovieApiJob, jobParameters);
    }

    @Scheduled(cron = "${batch.schedule.tv}")
    public void runTvSeriesJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();
        jobLauncher.run(getTvSeriesApiJob, jobParameters);
    }
}
