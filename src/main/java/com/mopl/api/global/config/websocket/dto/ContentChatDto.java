package com.mopl.api.global.config.websocket.dto;

import com.mopl.api.domain.user.dto.request.UserSummary;
import lombok.Builder;

@Builder
public record ContentChatDto(
    UserSummary sender,
    String content
) {

}