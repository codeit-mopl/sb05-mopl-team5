package com.mopl.api.domain.conversation.dto.response.conversation;


import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationWith(
    UUID userId,
    String name,
    String profileImageUrl

){

}