package com.mopl.api.global.client.sportdb;

import com.mopl.api.global.client.sportdb.response.EventsResponse;
import com.mopl.api.global.client.sportdb.response.SportResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ThesportdbApiClient {

    @Value("${thesportdb.api-key}")
    private String apiKey;

    private final RestClient restClient;

    public ThesportdbApiClient(RestClient.Builder builder) {
        this.restClient = builder
            .baseUrl("https://www.thesportsdb.com/api/v1/json")
            .build();
    }

    public List<SportResponse> getSportDetails(String date) {
        try {
            EventsResponse events = restClient.get()
                               .uri(uriBuilder ->
                                   uriBuilder.path("/" + apiKey + "/eventsday.php")
                                             .queryParam("d", date)
                                             .queryParam("s", "soccer")
                                             .queryParam("language", "en-En")
                                             .build()
                               )
                               .retrieve()
                               .body(EventsResponse.class);
            return events != null ? events.events() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch events from TheSportsDB for date=" + date, e);
            return List.of();
        }
    }
}
