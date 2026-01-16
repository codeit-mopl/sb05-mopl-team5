package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    // 1. 대화방의 모든 참여자 조회
    @Query("SELECT cp FROM ConversationParticipant cp " +
        "JOIN FETCH cp.user " +
        "WHERE cp.conversation.id = :conversationId")
    List<ConversationParticipant> findAllByConversationId(@Param("conversationId") UUID conversationId);


    // 2. 읽음 시간 업데이트
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ConversationParticipant cp
        SET cp.lastReadAt = :createdAt
        WHERE cp.conversation.id = :conversationId
          AND cp.user.id = :userId
          AND (cp.lastReadAt IS NULL OR cp.lastReadAt < :createdAt)
    """)
    void updateLastReadAtIfNewer(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId,
        @Param("createdAt") LocalDateTime createdAt
    );

    // 3. 참여자 검증
    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);


    // 4. 특정 유저의 '마지막 읽은 시간'만 조회
    @Query("SELECT cp.lastReadAt FROM ConversationParticipant cp " +
        "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    Optional<LocalDateTime> findLastReadAtByConversationIdAndUserId(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId
    );

    // =====================================================================
    // 5. [수정됨] 내 참여 정보 엔티티 조회 (명시적 쿼리 추가로 403 원인 차단)
    // =====================================================================
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    Optional<ConversationParticipant> findByConversationIdAndUserId(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId
    );

    // 6. 상대방 찾기
    @Query("""
        SELECT cp FROM ConversationParticipant cp
        JOIN FETCH cp.user
        WHERE cp.conversation.id = :conversationId
        AND cp.user.id <> :myId
    """)
    Optional<ConversationParticipant> findOtherParticipant(
        @Param("conversationId") UUID conversationId,
        @Param("myId") UUID myId
    );
}