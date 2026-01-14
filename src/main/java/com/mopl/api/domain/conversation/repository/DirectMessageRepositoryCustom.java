package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DirectMessageRepositoryCustom {

    /**
     * DM 목록 조회 (완전 최적 Seek Pagination)
     * - cursor(createdAt) + idAfter(UUID)
     * - limit+1 로 hasNext 판단
     */
    List<DirectMessage> findMessageList(
        UUID conversationId,
        LocalDateTime cursorTime,
        UUID idAfter,
        int limit,
        String sortDirection
    );

    long countMessageList(UUID conversationId);

    boolean existsParticipant(UUID conversationId, UUID userId);

    /**
     * 1:1 대화에서 sender를 제외한 상대방 참가자 1명 조회 (user fetchJoin)
     */
    ConversationParticipant findOtherParticipant(UUID conversationId, UUID senderId);
}
