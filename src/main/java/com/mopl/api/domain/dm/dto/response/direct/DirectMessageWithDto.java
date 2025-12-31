package com.mopl.api.domain.dm.dto.response.direct;


import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageWithDto(

    UUID id,
    DirectMessageWith with,
    DirectMessageLastestMessage lastestMessage,
    boolean hasUnread

) {

}
