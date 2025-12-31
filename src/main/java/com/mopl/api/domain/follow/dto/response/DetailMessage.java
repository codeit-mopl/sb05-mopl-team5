package com.mopl.api.domain.follow.dto.response;


import lombok.Builder;

@Builder
public record DetailMessage(
    String additionalProp1,
    String additionalProp2,
    String additionalProp3
) {

}
