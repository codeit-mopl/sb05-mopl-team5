package com.mopl.api.domain.conversation.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public class ConversationNotFoundException extends RuntimeException {

    private final UUID withUserId;

    public ConversationNotFoundException(UUID withUserId) {
        super("대화를 찾을 수 없습니다.");
        this.withUserId = withUserId;
    }
}
