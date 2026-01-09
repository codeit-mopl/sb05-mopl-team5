package com.mopl.api.domain.dm.dto.response.conversation;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationLatestMessage(

    UUID id,
    UUID conversationsId,
    LocalDateTime createdAt,
    ConversationSend sender,
    ConversationReceiver receiver,
    String content
) {


    // [핵심] QueryDSL 에러 해결을 위한 추가 생성자
    // 에러 로그에 나온 순서: [String, LocalDateTime] 그대로 받아줍니다.
    public ConversationLatestMessage(String content, LocalDateTime createdAt) {
        // this(id, conversationsId, createdAt, sender, receiver, content)
        // 없는 값은 null로 채우고, 받은 값은 제자리에 넣어줍니다.
        this(null, null, createdAt, null, null, content);
    }

}