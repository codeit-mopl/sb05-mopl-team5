package com.mopl.api.global.client.sportdb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventsResponse(
    @JsonProperty("events")
    List<SportResponse> events
) {}
