package com.mopl.api.batch.job;

import com.mopl.api.batch.processor.MovieProcessor;
import com.mopl.api.batch.reader.MovieReader;
import com.mopl.api.batch.writer.TmdbWriter;
import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.global.client.tmdb.response.MovieResponse;
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
public class MovieApiJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MovieReader movieReader;
    private final MovieProcessor movieProcessor;
    private final TmdbWriter tmdbWriter;

    @Bean
    public Job getMovieApiJob() {
        return new JobBuilder("getMovieApiJob", jobRepository)
            .start(getMovieApiStep())
            .build();

    }

    @Bean
    public Step getMovieApiStep() {
        return new StepBuilder("getMovieApiStep", jobRepository)
            .<MovieResponse, Content>chunk(10, transactionManager)
            .reader(movieReader)
            .processor(movieProcessor)
            .writer(tmdbWriter)
            .build();
    }
}
