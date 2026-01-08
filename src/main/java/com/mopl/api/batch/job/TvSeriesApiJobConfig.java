package com.mopl.api.batch.job;

import com.mopl.api.batch.processor.TvSeriesProcessor;
import com.mopl.api.batch.reader.TvSeriesReader;
import com.mopl.api.batch.writer.TmdbWriter;
import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.global.client.tmdb.response.TvSeriesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class TvSeriesApiJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TvSeriesReader tvSeriesReader;
    private final TvSeriesProcessor tvSeriesProcessor;
    private final TmdbWriter tmdbWriter;

    @Bean
    public Job getTvSeriesApiJob() {
        return new JobBuilder("getTvSeriesApiJob", jobRepository)
            .start(getTvSeriesApiStep())
            .build();
    }

    @Bean
    public Step getTvSeriesApiStep() {
        return new StepBuilder("getSportApiStep", jobRepository)
            .<TvSeriesResponse, Content>chunk(10, transactionManager)
            .reader(tvSeriesReader)
            .processor(tvSeriesProcessor)
            .writer(tmdbWriter)
            .build();
    }

}
