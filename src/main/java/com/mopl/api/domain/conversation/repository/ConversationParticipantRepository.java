package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

}