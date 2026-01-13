package com.mopl.api.domain.conversation.entity;


import com.mopl.api.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseEntity {


    // [추가] 역정규화 컬럼 매핑
    @Column(name = "last_message_content", length = 2000)
    private String lastMessageContent;

    @Column(name = "last_message_created_at")
    private LocalDateTime lastMessageCreatedAt;
    @Column(name = "last_message_sender_id")
    private UUID lastMessageSenderId;

    @Column(name = "last_message_id")
    private UUID lastMessageId;

    public  static  Conversation create() {
        return new Conversation();
    }

    // [추가] 메시지 전송 시 호출하여 대화방 정보를 갱신하는 메서드
    public void updateLastMessage(UUID messageId, String content, LocalDateTime createdAt, UUID senderId) {
        this.lastMessageId = messageId;       // 저장!
        this.lastMessageContent = content;
        this.lastMessageCreatedAt = createdAt;
        this.lastMessageSenderId = senderId;  // 저장!
    }


}