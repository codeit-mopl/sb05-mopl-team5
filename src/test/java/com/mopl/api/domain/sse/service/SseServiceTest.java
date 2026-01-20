package com.mopl.api.domain.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mopl.api.domain.sse.entity.SseMessage;
import com.mopl.api.domain.sse.repository.SseEmitterRepository;
import com.mopl.api.domain.sse.repository.SseMessageRepository;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

    @InjectMocks
    private SseService sseService;

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @Mock
    private SseMessageRepository sseMessageRepository;

    @Test
    @DisplayName("connect: 초기 연결 및 Emitter 저장 확인")
    void connect_Success() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        SseEmitter emitter = sseService.connect(userId, null);

        // then
        assertThat(emitter).isNotNull();
        verify(sseEmitterRepository).save(eq(userId), any(SseEmitter.class));
    }

    @Test
    @DisplayName("connect: 지난 이벤트 재전송 확인")
    void connect_ReplayEvents() {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();

        SseMessage missedMessage = SseMessage.create(Set.of(userId), "missed", "data");
        given(sseMessageRepository.findAllByEventIdAfterAndReceiverId(lastEventId, userId))
            .willReturn(List.of(missedMessage));

        // when
        sseService.connect(userId, lastEventId);

        // then
        verify(sseEmitterRepository).save(eq(userId), any(SseEmitter.class));
        verify(sseMessageRepository).findAllByEventIdAfterAndReceiverId(lastEventId, userId);
    }

    @Test
    @DisplayName("send: 메시지 저장 및 전송 확인")
    void send_Success() {
        // given
        UUID receiverId = UUID.randomUUID();
        Set<UUID> receivers = Set.of(receiverId);
        String eventName = "test-event";
        String data = "test-data";

        SseEmitter mockEmitter = mock(SseEmitter.class);
        given(sseEmitterRepository.findAllByReceiverIdsIn(receivers)).willReturn(List.of(mockEmitter));

        SseMessage savedMessage = SseMessage.create(receivers, eventName, data);
        given(sseMessageRepository.save(any(SseMessage.class))).willReturn(savedMessage);

        // when
        sseService.send(receivers, eventName, data);

        // then
        verify(sseMessageRepository).save(any(SseMessage.class));
        try {
            verify(mockEmitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        } catch (IOException e) {
        }
    }
}