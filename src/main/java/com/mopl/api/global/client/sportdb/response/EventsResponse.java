package com.mopl.api.global.client.sportdb.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EventsResponse(
    @JsonProperty("events")
    List<SportResponse> events
) {}
