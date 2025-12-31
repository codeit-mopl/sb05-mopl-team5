package com.mopl.api.domain.dm.dto.response.direct;


import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageReceiver(
    UUID userid,
    String name,
    String profileImageUrl
) {

}
