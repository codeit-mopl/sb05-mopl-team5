package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.DirectMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DirectMessageRepositoryCustom {

    List<DirectMessage> findMessageList(
        UUID conversationId,
        LocalDateTime cursorTime,
        UUID idAfter,
        int limit,
        String sortDirection
    );
}
