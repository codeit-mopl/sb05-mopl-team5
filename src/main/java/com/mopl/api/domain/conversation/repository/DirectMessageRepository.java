package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.DirectMessage;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID>, DirectMessageRepositoryCustom {

    @Query("SELECT m FROM DirectMessage m " +
        "JOIN FETCH m.sender " +
        "JOIN FETCH m.receiver " +
        "WHERE m.conversation.id = :conversationId " +
        "ORDER BY m.createdAt DESC LIMIT 1")
    Optional<DirectMessage> findLatestByConversationId(@Param("conversationId") UUID conversationId);


    @Query("SELECT m.createdAt FROM DirectMessage m " +
        "WHERE m.id = :messageId AND m.conversation.id = :conversationId")
    Optional<LocalDateTime> findCreatedAtByIdAndConversationId(
        @Param("messageId") UUID messageId,
        @Param("conversationId") UUID conversationId
    );
}