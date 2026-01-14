package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    @Query("SELECT cp FROM ConversationParticipant cp " +
        "JOIN FETCH cp.user " +
        "WHERE cp.conversation.id = :conversationId")
    List<ConversationParticipant> findAllByConversationId(@Param("conversationId") UUID conversationId);


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

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);



    @Query("SELECT cp.lastReadAt FROM ConversationParticipant cp " +
        "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    Optional<LocalDateTime> findLastReadAtByConversationIdAndUserId(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId
    );

    UUID user(User user);
}