package com.mopl.api.domain.content.service;

import com.mopl.api.domain.user.dto.request.UserSummary;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.global.config.websocket.dto.ContentChatDto;
import com.mopl.api.global.config.websocket.dto.ContentChatSendRequest;
import com.mopl.api.global.config.websocket.publisher.RedisContentChatPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentChatServiceImpl implements ContentChatService {

    private final UserRepository userRepository;
    private final RedisContentChatPublisher publisher;

    @Override
    public void sendChat(UUID contentId, UUID senderId, ContentChatSendRequest request) {

        User user = userRepository.findById(senderId)
                                  .orElseThrow(); // TODO User Not Found

        UserSummary sender = UserSummary.builder()
                                        .userId(user.getId())
                                        .name(user.getName())
                                        .profileImageUrl(
                                            user.getProfileImageUrl() != null ? user.getProfileImageUrl() : null)
                                        .build();

        ContentChatDto dto = ContentChatDto.builder()
                                           .sender(sender)
                                           .content(request.content())
                                           .build();

        log.debug("채팅 이벤트 발행: {}", dto);
        publisher.publish(contentId, dto);
    }
}