package com.mopl.api.domain.sse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.api.domain.sse.entity.SseMessage;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseRepositoryTest {

    @Test
    @DisplayName("EmitterRepository: 저장, 조회, 삭제 테스트")
    void emitterRepository_Logic() {
        // given
        SseEmitterRepositoryImpl repository = new SseEmitterRepositoryImpl();
        UUID userId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();

        // when: 저장
        repository.save(userId, emitter);

        // then: 조회
        List<SseEmitter> found = repository.findByReceiverId(userId)
                                           .orElseThrow();
        assertThat(found).contains(emitter);

        // when: 삭제
        repository.delete(userId, emitter);

        // then: 삭제 확인
        assertThat(repository.findByReceiverId(userId)).isEmpty();
    }

    @Test
    @DisplayName("MessageRepository: 큐 용량 제한 및 메시지 조회 테스트")
    void messageRepository_Logic() {
        // given
        int capacity = 3;
        SseMessageRepositoryImpl repository = new SseMessageRepositoryImpl(capacity);
        UUID receiverId = UUID.randomUUID();

        SseMessage msg1 = saveMessage(repository, receiverId, "1");
        SseMessage msg2 = saveMessage(repository, receiverId, "2");
        SseMessage msg3 = saveMessage(repository, receiverId, "3");
        SseMessage msg4 = saveMessage(repository, receiverId, "4");

        List<SseMessage> result = repository.findAllByEventIdAfterAndReceiverId(msg2.getEventId(), receiverId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("data")
                          .containsExactly("3", "4");
    }

    private SseMessage saveMessage(SseMessageRepositoryImpl repo, UUID receiverId, Object data) {
        SseMessage msg = SseMessage.create(Set.of(receiverId), "test", data);
        return repo.save(msg);
    }
}