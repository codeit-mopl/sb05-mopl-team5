package com.mopl.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.mopl.api.domain")
@EntityScan(basePackages = "com.mopl.api.domain")
public class MoplApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoplApplication.class, args);
    }

}