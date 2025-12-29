package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.Conversation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

}