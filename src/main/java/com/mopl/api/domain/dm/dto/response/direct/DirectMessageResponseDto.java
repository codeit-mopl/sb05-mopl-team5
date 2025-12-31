package com.mopl.api.domain.dm.dto.response.direct;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageResponseDto(

    List<DirectMessageDto> data,

    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    int totalCount,
    String sortBy,
    String sortDirection

) {

}
