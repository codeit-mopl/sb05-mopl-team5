package com.mopl.api.domain.conversation.entity;

import com.mopl.api.domain.user.entity.User;
import com.mopl.api.global.common.entity.BaseUpdatableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "conversation_participants")
@Getter
public class ConversationParticipant extends BaseUpdatableEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime lastReadAt;
}
