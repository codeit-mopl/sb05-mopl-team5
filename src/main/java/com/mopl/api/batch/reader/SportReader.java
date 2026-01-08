package com.mopl.api.batch.reader;

import com.mopl.api.global.client.sportdb.ThesportdbApiClient;
import com.mopl.api.global.client.sportdb.response.SportResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SportReader {

    private final ThesportdbApiClient sportApiClient;

    @Bean
    @StepScope
    public ItemReader<SportResponse> read(@Value("#{jobParameters['date']}") String date) {
        return new ItemReader<>() {

            private int index = 0;
            private List<SportResponse> sports;
            private final Set<Long> seenApiIds = new HashSet<>();

            @Override
            public SportResponse read() {
                if (sports == null) {
                    sports = sportApiClient.getSportDetails(date);
                }

                while (index < sports.size()) {
                    SportResponse next = sports.get(index++);
                    Long apiId = next.apiId();

                    if (seenApiIds.add(apiId)) {
                        return next;
                    }
                }
                return null;
            }
        };
    }
}
