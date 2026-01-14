package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.dto.response.conversation.ConversationListRow;
import com.mopl.api.domain.conversation.entity.Conversation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable; // Pageable 사용
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 🔥 [핵심 1] extends ConversationRepositoryCustom 제거!
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    // 1. 1:1 대화방 ID 찾기 (기존 유지)
    @Query("SELECT c.id FROM Conversation c " +
        "JOIN c.participants p1 " +
        "JOIN c.participants p2 " +
        "WHERE p1.user.id IN :userIds AND p2.user.id IN :userIds " +
        "GROUP BY c.id " +
        "HAVING COUNT(DISTINCT p1.user.id) = 2")
    Optional<UUID> findOneToOneConversationId(@Param("userIds") Set<UUID> userIds);

    // 2. 대화방 목록 조회 (QueryDSL -> JPQL 변환)
    // 🔥 [핵심 2] new 패키지경로.Record명(...) 문법 사용
    // 주의: ConversationListRow의 필드 순서와 일치해야 합니다.
    @Query("""
    SELECT new com.mopl.api.domain.conversation.dto.response.conversation.ConversationListRow(
        c.id,
        otherUser.id,
        otherUser.name,
        otherUser.profileImageUrl,
        
        c.lastMessageId,
        c.lastMessageContent,
        c.lastMessageCreatedAt,
        
        (SELECT COUNT(dm) 
         FROM DirectMessage dm 
         WHERE dm.conversation.id = c.id 
           AND dm.createdAt > myP.lastReadAt 
           AND dm.sender.id <> :me
        )
    )
    FROM Conversation c
    JOIN ConversationParticipant myP ON c.id = myP.conversation.id
    JOIN ConversationParticipant otherP ON c.id = otherP.conversation.id
    JOIN otherP.user otherUser
    WHERE myP.user.id = :me
      AND otherP.user.id <> :me
      AND (:keyword IS NULL OR otherUser.name LIKE %:keyword%)
      AND (:cursorTime IS NULL OR c.lastMessageCreatedAt < :cursorTime)
    ORDER BY c.lastMessageCreatedAt DESC
""")
    List<ConversationListRow> findConversationList(
        @Param("me") UUID me,
        @Param("keyword") String keyword,
        @Param("cursorTime") LocalDateTime cursorTime,
        Pageable pageable
    );

    // 3. 개수 조회 (QueryDSL -> JPQL 변환)
    @Query("""
        SELECT COUNT(c)
        FROM Conversation c
        JOIN ConversationParticipant myP ON c.id = myP.conversation.id
        JOIN ConversationParticipant otherP ON c.id = otherP.conversation.id
        JOIN otherP.user otherUser
        WHERE myP.user.id = :me
          AND otherP.user.id <> :me
          AND (:keyword IS NULL OR otherUser.name LIKE %:keyword%)
    """)
    long countConversationList(
        @Param("me") UUID me,
        @Param("keyword") String keyword
    );
}