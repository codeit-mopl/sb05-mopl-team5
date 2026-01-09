package com.mopl.api.domain.conversation.controller;


import com.mopl.api.domain.conversation.dto.request.DirectMessageSendRequest;
import com.mopl.api.domain.conversation.service.DirectMessageCommandService;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectMessageSocketController {


    private final DirectMessageCommandService directMessageCommandService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/conversations/{conversationId}/direct-messages")
    public void send(
        @DestinationVariable UUID conversationId,
        @Valid DirectMessageSendRequest request,
        Principal principal
    ) {
        UUID senderId = extractUserId(principal);

        DirectMessageDto saved = directMessageCommandService.send(
            conversationId,
            senderId,
            request.content()
        );

        // ✅ 구독자들에게 broadcast (활성 대화는 WS로 받음)
        messagingTemplate.convertAndSend(
            "/sub/conversations/" + conversationId + "/direct-messages",
            saved
        );
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            Object p = token.getPrincipal();
            if (p instanceof UUID uuid) return uuid;
            return UUID.fromString(p.toString());
        }
        return UUID.fromString(principal.getName());
    }

}
