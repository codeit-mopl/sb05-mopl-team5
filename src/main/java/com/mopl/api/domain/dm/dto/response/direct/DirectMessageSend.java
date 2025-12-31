package com.mopl.api.domain.dm.dto.response.direct;


import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageSend (
    UUID userId,
    String name,
    String profileImageUrl

){

}
