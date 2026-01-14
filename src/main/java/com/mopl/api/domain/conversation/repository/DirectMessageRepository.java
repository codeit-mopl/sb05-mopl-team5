package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.DirectMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    // =================================================================
    // 1. 기존에 있던 메서드들 (유지)
    // =================================================================

    // JPQL에는 LIMIT 키워드가 표준이 아니므로 Pageable을 쓰는 게 정석이지만,
    // 현재 작동한다면 두셔도 됩니다. (혹은 findTopBy...OrderBy... 사용 권장)
    @Query("SELECT m FROM DirectMessage m " +
        "JOIN FETCH m.sender " +
        "JOIN FETCH m.receiver " +
        "WHERE m.conversation.id = :conversationId " +
        "ORDER BY m.createdAt DESC")
    List<DirectMessage> findLatestList(@Param("conversationId") UUID conversationId, Pageable pageable);
    // 꿀팁: 위 메서드를 호출할 때 PageRequest.of(0, 1)을 넘기면 LIMIT 1과 같습니다.

    @Query("SELECT m.createdAt FROM DirectMessage m " +
        "WHERE m.id = :messageId AND m.conversation.id = :conversationId")
    Optional<LocalDateTime> findCreatedAtByIdAndConversationId(
        @Param("messageId") UUID messageId,
        @Param("conversationId") UUID conversationId
    );

    // =================================================================
    // 2. QueryDSL 대체 메서드 (추가됨)
    // =================================================================

    // ① 전체 개수 조회 (countMessageList 대체)
    long countByConversationId(UUID conversationId);

    // 1. 내림차순 (DESC)
    @Query("SELECT m FROM DirectMessage m " +
        "JOIN FETCH m.sender " +
        "JOIN FETCH m.receiver " +
        "WHERE m.conversation.id = :conversationId " +
        "AND (" +
        "    :cursorTime IS NULL " +
        "    OR m.createdAt < :cursorTime " +
        "    OR (m.createdAt = :cursorTime AND m.id < :idAfter)" +
        ") " +
        "ORDER BY m.createdAt DESC, m.id DESC")
    List<DirectMessage> findMessageListDesc(
        @Param("conversationId") UUID conversationId,
        @Param("cursorTime") LocalDateTime cursorTime,
        @Param("idAfter") UUID idAfter,
        Pageable pageable
    );

    // 2. 오름차순 (ASC)
    @Query("SELECT m FROM DirectMessage m " +
        "JOIN FETCH m.sender " +
        "JOIN FETCH m.receiver " +
        "WHERE m.conversation.id = :conversationId " +
        "AND (" +
        "    :cursorTime IS NULL " +
        "    OR m.createdAt > :cursorTime " +
        "    OR (m.createdAt = :cursorTime AND m.id > :idAfter)" +
        ") " +
        "ORDER BY m.createdAt ASC, m.id ASC")
    List<DirectMessage> findMessageListAsc(
        @Param("conversationId") UUID conversationId,
        @Param("cursorTime") LocalDateTime cursorTime,
        @Param("idAfter") UUID idAfter,
        Pageable pageable
    );


    Optional<DirectMessage> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}