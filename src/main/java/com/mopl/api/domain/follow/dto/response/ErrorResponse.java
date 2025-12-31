package com.mopl.api.domain.follow.dto.response;


import lombok.Builder;

@Builder
public record ErrorResponse(

    String exceptionName,
    String message,
    DetailMessage detail

) {

}


