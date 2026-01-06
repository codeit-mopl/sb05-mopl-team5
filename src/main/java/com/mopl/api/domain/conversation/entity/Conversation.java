package com.mopl.api.domain.conversation.entity;


import com.mopl.api.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor()
public class Conversation extends BaseEntity {


    // [추가] 역정규화 컬럼 매핑
    @Column(name = "last_message_content", length = 1000)
    private String lastMessageContent;

    @Column(name = "last_message_created_at")
    private LocalDateTime lastMessageCreatedAt;

    // [추가] 메시지 전송 시 호출하여 대화방 정보를 갱신하는 메서드
    public void updateLastMessage(String content, LocalDateTime createdAt) {
        this.lastMessageContent = content;
        this.lastMessageCreatedAt = createdAt;
    }
}