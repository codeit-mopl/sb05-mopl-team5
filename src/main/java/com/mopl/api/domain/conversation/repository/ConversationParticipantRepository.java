package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    @Query("SELECT cp FROM ConversationParticipant cp " +
        "JOIN FETCH cp.user " +
        "WHERE cp.conversation.id = :conversationId")
    List<ConversationParticipant> findAllByConversationId(@Param("conversationId") UUID conversationId);


    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationParticipant p SET p.lastReadAt = :readAt " +
        "WHERE p.conversation.id = :conversationId " +
        "AND p.user.id = :userId " +
        "AND (p.lastReadAt IS NULL OR p.lastReadAt < :readAt)")
    void updateLastReadAtIfNewer(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId,
        @Param("readAt") LocalDateTime readAt
    );

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);



    @Query("SELECT cp.lastReadAt FROM ConversationParticipant cp " +
        "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    Optional<LocalDateTime> findLastReadAtByConversationIdAndUserId(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId
    );

}