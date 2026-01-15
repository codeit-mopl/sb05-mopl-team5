package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.dto.response.conversation.ConversationSummary;
import com.mopl.api.domain.conversation.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    // 1. 1:1 대화방 조회 (엄격한 모드: 정확히 두 명만 있는 방)
    @Query("""
        SELECT c.id 
        FROM Conversation c
        JOIN c.participants p
        WHERE p.user.id IN :userIds
        GROUP BY c.id
        HAVING COUNT(DISTINCT p.user.id) = 2
           AND (SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation.id = c.id) = 2
    """)
    Optional<UUID> findOneToOneConversationId(@Param("userIds") Set<UUID> userIds);

    // 2. 목록 조회 (Interface Projection 사용)
    @Query("""
        SELECT 
            c.id AS conversationId,
            otherUser.id AS otherUserId,
            otherUser.name AS otherName,
            otherUser.profileImageUrl AS otherProfileImageUrl,
            
            c.lastMessageId AS lastMessageId,
            c.lastMessageContent AS lastMessageContent,
            c.lastMessageCreatedAt AS lastMessageCreatedAt,
            
            (SELECT COUNT(dm) 
             FROM DirectMessage dm 
             WHERE dm.conversation.id = c.id 
               AND (myP.lastReadAt IS NULL OR dm.createdAt > myP.lastReadAt)
               AND dm.sender.id <> :me
            ) AS unreadCount
        FROM Conversation c
        JOIN ConversationParticipant myP ON c.id = myP.conversation.id
        JOIN ConversationParticipant otherP ON c.id = otherP.conversation.id
        JOIN otherP.user otherUser
        WHERE myP.user.id = :me
          AND otherP.user.id <> :me
          AND (:keyword IS NULL OR otherUser.name LIKE CONCAT('%', :keyword, '%'))
          AND (:cursorTime IS NULL OR c.lastMessageCreatedAt < :cursorTime)
        ORDER BY c.lastMessageCreatedAt DESC
    """)
    List<ConversationSummary> findConversationList(
        @Param("me") UUID me,
        @Param("keyword") String keyword,
        @Param("cursorTime") LocalDateTime cursorTime,
        Pageable pageable
    );

    // 3. 개수 조회 (LIKE 문법 수정 완료)
    @Query("""
        SELECT COUNT(c)
        FROM Conversation c
        JOIN ConversationParticipant myP ON c.id = myP.conversation.id
        JOIN ConversationParticipant otherP ON c.id = otherP.conversation.id
        JOIN otherP.user otherUser
        WHERE myP.user.id = :me
          AND otherP.user.id <> :me
          AND (:keyword IS NULL OR otherUser.name LIKE CONCAT('%', :keyword, '%'))
    """)
    long countConversationList(
        @Param("me") UUID me,
        @Param("keyword") String keyword
    );
}