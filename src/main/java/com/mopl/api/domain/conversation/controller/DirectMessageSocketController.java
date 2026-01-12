package com.mopl.api.domain.conversation.controller;


import com.mopl.api.domain.conversation.dto.request.DirectMessageSendRequest;
import com.mopl.api.domain.conversation.service.DirectMessageCommandService;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectMessageSocketController {


    private final DirectMessageCommandService directMessageCommandService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/conversations/{conversationId}/direct-messages")
    public void send(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @DestinationVariable UUID conversationId,
        @Payload DirectMessageSendRequest request

    ) {
        UUID senderId = userDetails.getUserDto().id();

        DirectMessageDto saved = directMessageCommandService.send(
            conversationId,
            senderId,
            request
        );

        // ✅ 구독자들에게 broadcast (활성 대화는 WS로 받음)
        messagingTemplate.convertAndSend(
            "/sub/conversations/" + conversationId + "/direct-messages",
            saved
        );
    }


}
