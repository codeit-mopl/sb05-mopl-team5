package com.mopl.api.domain.content.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentType {
    MOVIE("movie"),
    TV_SERIES("tvSeries"),
    SPORT("sport");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ContentType findByValue(String value) {
        if (value == null) {
            return null;
        }

        for (ContentType type : ContentType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ContentType: " + value);
    }
}
