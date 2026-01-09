package com.mopl.api.domain.conversation.dto.response.direct;


import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageWith(
    UUID userId,
    String name,
    String profileImageUrl
) {

}
