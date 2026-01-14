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

    // 1. 대화방의 모든 참여자 조회 (User Fetch Join) -> conversationCheck 메서드에서 사용
    @Query("SELECT cp FROM ConversationParticipant cp " +
        "JOIN FETCH cp.user " +
        "WHERE cp.conversation.id = :conversationId")
    List<ConversationParticipant> findAllByConversationId(@Param("conversationId") UUID conversationId);


    // 2. 읽음 시간 업데이트 (성능 최적화: 더 최신일 경우에만 update 쿼리 실행)
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

    // 3. 참여자 검증 (boolean 반환) -> existsParticipant 대체
    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);


    // 4. 특정 유저의 '마지막 읽은 시간'만 조회 (최적화)
    @Query("SELECT cp.lastReadAt FROM ConversationParticipant cp " +
        "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    Optional<LocalDateTime> findLastReadAtByConversationIdAndUserId(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId
    );

    // =====================================================================
    // 🔥 [추가] QueryDSL 제거 후 서비스 코드에서 호출하는 신규 메서드들
    // =====================================================================

    // 5. 내 참여 정보 엔티티 조회 (createConversation - Case 1 에서 사용)
    // findLastReadAt... 은 시간만 가져오지만, 이건 객체 전체를 가져옵니다.
    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    // 6. 상대방 찾기 (findOtherParticipant 대체)
    // "이 대화방에서 내가 아닌 다른 사람(들)을 User 정보와 함께 가져와라"
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