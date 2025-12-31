package com.mopl.api.domain.dm.dto.request;


import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationRequestDto(
    UUID withUserId
) {

}
