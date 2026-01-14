package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConversationRepository extends JpaRepository<Conversation, UUID>, ConversationRepositoryCustom {


    @Query("SELECT cp.conversation.id FROM ConversationParticipant cp " +
        "WHERE cp.user.id IN :userIds " +
        "GROUP BY cp.conversation.id " +
        "HAVING COUNT(DISTINCT cp.user.id) = 2")
    Optional<UUID> findOneToOneConversationId(@Param("userIds") Set<UUID> userIds);
}