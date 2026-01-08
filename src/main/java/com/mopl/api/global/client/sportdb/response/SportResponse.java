package com.mopl.api.global.client.sportdb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportResponse(
    @JsonProperty("idEvent")
    Long apiId,

    @JsonProperty("strEvent")
    String title,

    @JsonProperty("strFilename")
    String description,

    @JsonProperty("strThumb")
    String thumbnailUrl,

    @JsonProperty("strSport")
    String sport,

    @JsonProperty("strVenue")
    String venue
) {

    public String getTags() {
        return sport + "|" + venue;
    }
}
