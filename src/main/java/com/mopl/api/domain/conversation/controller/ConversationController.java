package com.mopl.api.domain.conversation.controller;

import com.mopl.api.domain.conversation.dto.request.ConversationRequestDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageResponseDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWithDto;
import com.mopl.api.domain.conversation.service.ConversationService;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService service;


    @GetMapping
    public ResponseEntity<ConversationResponseDto> getConversationList(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) String keywordLike,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "DESCENDING") String sortDirection,
        @RequestParam(defaultValue = "lastMessageCreatedAt") String sortBy
    ) {
        UUID me = userDetails.getUserDto().id();
        return ResponseEntity.ok(
            service.getConversationList(me, keywordLike, cursor, idAfter, limit, sortDirection, sortBy)
        );
    }


    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody @Valid ConversationRequestDto request
    ) {
        UUID me = userDetails.getUserDto().id();
        ConversationDto dto = service.createConversation(me, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }


    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable UUID conversationId
    ) {
        UUID me = userDetails.getUserDto().id();
        return ResponseEntity.ok(service.conversationCheck(me, conversationId));
    }


    @GetMapping("/{conversationId}/direct-messages")
    public ResponseEntity<DirectMessageResponseDto> getDirectMessageList(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable UUID conversationId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "ASCENDING") String sortDirection,
        @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        UUID me = userDetails.getUserDto().id();
        return ResponseEntity.ok(
            service.getDirectMessageList(me, conversationId, cursor, idAfter, limit, sortDirection, sortBy)
        );
    }


    @GetMapping("/with")
    public ResponseEntity<DirectMessageWithDto> getDirectMessageWith(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam UUID userId
    ) {
        UUID me = userDetails.getUserDto().id();
        DirectMessageWithDto dto = service.getDirectMessageWith(me, userId);

        if (dto == null || dto.id() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(dto);
    }


//    @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
//    public ResponseEntity<Void> readConversation(
//        @AuthenticationPrincipal CustomUserDetails userDetails,
//        @PathVariable UUID conversationId,
//        @PathVariable UUID directMessageId
//    ) {
//        UUID me = userDetails.getUserDto().id();
//        service.conversationRead(me, conversationId, directMessageId);
//        return ResponseEntity.noContent().build();
//    }


    @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
    public ResponseEntity<Void> directMessageRead(
        @PathVariable UUID conversationId,
        @PathVariable UUID directMessageId) {
        service.conversationRead(conversationId, directMessageId);
        return ResponseEntity.noContent().build();
    }
}
