package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.DirectMessage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

}