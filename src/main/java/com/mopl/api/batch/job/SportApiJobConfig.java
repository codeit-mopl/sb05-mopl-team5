package com.mopl.api.batch.job;

import com.mopl.api.batch.processor.SportProcessor;
import com.mopl.api.batch.writer.SportWriter;
import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.global.client.sportdb.response.SportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SportApiJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ItemReader<SportResponse> sportReader;
    private final SportProcessor sportProcessor;
    private final SportWriter sportWriter;

    @Bean
    public Job getSportApiJob() {
        return new JobBuilder("getSportApiJob", jobRepository)
            .start(getSportApiStep())
            .build();
    }

    @Bean
    public Step getSportApiStep() {
        return new StepBuilder("getSportApiStep", jobRepository)
            .<SportResponse, Content>chunk(10, transactionManager)
            .reader(sportReader)
            .processor(sportProcessor)
            .writer(sportWriter)
            .build();
    }
}
