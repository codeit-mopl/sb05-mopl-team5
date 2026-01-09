package com.mopl.api.domain.conversation.dto.response.conversation;


import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationResponseDto(
    List<ConversationDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    int totalCount,
    String sortBy,
    String sortDirection
) {

}