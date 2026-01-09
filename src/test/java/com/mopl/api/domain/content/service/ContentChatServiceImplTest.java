package com.mopl.api.domain.content.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRedisRepository;
import com.mopl.api.global.config.websocket.dto.ContentChatDto;
import com.mopl.api.global.config.websocket.dto.ContentChatSendRequest;
import com.mopl.api.global.config.websocket.publisher.RedisContentChatPublisher;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentChatServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WatchingSessionRedisRepository watchingSessionRedisRepository;

    @Mock
    private RedisContentChatPublisher publisher;

    @InjectMocks
    private ContentChatServiceImpl contentChatService;

    private UUID contentId;
    private UUID senderId;
    private User user;

    @BeforeEach
    void setUp() {
        contentId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        user = mock(User.class);
    }

    @Test
    @DisplayName("성공: 시청 중인 유저가 채팅을 보내면 Redis에 발행된다")
    void sendChat_Success() {

        ContentChatSendRequest request = new ContentChatSendRequest("안녕하세요!");

        given(userRepository.findById(senderId)).willReturn(Optional.of(user));
        given(watchingSessionRedisRepository.isWatching(contentId, senderId)).willReturn(true);

        contentChatService.sendChat(contentId, senderId, request);

        then(publisher).should().publish(eq(contentId), any(ContentChatDto.class));
    }

    @Test
    @DisplayName("실패: 시청 중이 아닌 유저가 채팅을 보내면 무시된다")
    void sendChat_Fail_WhenNotWatching() {

        ContentChatSendRequest request = new ContentChatSendRequest("무시될 채팅");

        given(userRepository.findById(senderId)).willReturn(Optional.of(user));
        given(watchingSessionRedisRepository.isWatching(contentId, senderId)).willReturn(false);

        contentChatService.sendChat(contentId, senderId, request);

        then(publisher).should(never()).publish(any(), any());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 유저라면 예외가 발생한다")
    void sendChat_Fail_UserNotFound() {
        ContentChatSendRequest request = new ContentChatSendRequest("에러 채팅");
        given(userRepository.findById(senderId)).willReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
            contentChatService.sendChat(contentId, senderId, request)
        );

        then(watchingSessionRedisRepository).shouldHaveNoInteractions();
        then(publisher).shouldHaveNoInteractions();
    }
}