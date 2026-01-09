package com.mopl.api.batch.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialBatchRunner implements ApplicationRunner {

    private final ApiBatchScheduler apiBatchScheduler;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        apiBatchScheduler.runSportJob();
        apiBatchScheduler.runMovieJob();
        apiBatchScheduler.runTvSeriesJob();
    }
}
