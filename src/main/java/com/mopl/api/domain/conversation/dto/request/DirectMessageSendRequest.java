package com.mopl.api.domain.conversation.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record DirectMessageSendRequest(
    @NotBlank
    @Size(max = 1000)
    String content
) {

}
