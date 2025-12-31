package com.mopl.api.domain.dm.controller;


import com.mopl.api.domain.dm.dto.request.ConversationRequestDto;
import com.mopl.api.domain.dm.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.dm.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.dm.dto.response.direct.DirectMessageResponseDto;
import com.mopl.api.domain.dm.dto.response.direct.DirectMessageWithDto;
import com.mopl.api.domain.dm.service.ConversationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

  private final ConversationService service;


  @GetMapping
  public ResponseEntity<ConversationResponseDto> getConversationList(
      @RequestParam(required = false) String keywordLike,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam(defaultValue = "32") int limit,
      @RequestParam(defaultValue = "ASCENDING") String sortDirection,
      @RequestParam(defaultValue = "createdAt") String sortBy) {
    ConversationResponseDto conversationResponseDto = service.getConversationList(
        keywordLike, cursor, idAfter, limit, sortDirection, sortBy);
    return ResponseEntity.status(HttpStatus.OK).body(conversationResponseDto);
  }

  @PostMapping
  public ResponseEntity<ConversationDto> createConversation(@Valid @RequestBody ConversationRequestDto requestDto) {
    ConversationDto conversationDto = service.createConversation(requestDto);
    return ResponseEntity.status(HttpStatus.OK).body(conversationDto);
  }


  @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
  public ResponseEntity<Void> directMessageRead(@PathVariable UUID conversationId, @PathVariable UUID directMessageId) {
    service.conversationRead(conversationId, directMessageId);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @GetMapping("/{conversationId}")
  public ResponseEntity<ConversationDto> conversationCheck(@PathVariable UUID conversationId) {
    ConversationDto conversationDto = service.conversationCheck(conversationId);
    return ResponseEntity.status(HttpStatus.OK).body(conversationDto);
  }

  @GetMapping("/{conversationId}/direct-message")
  public ResponseEntity<DirectMessageResponseDto> getDirectMessageList(
      @PathVariable UUID conversationId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam(defaultValue = "32") int limit,
      @RequestParam(defaultValue = "ASCENDING") String sortDirection,
      @RequestParam(defaultValue = "createdAt") String sortBy) {

    DirectMessageResponseDto responseDto = service.getDirectMessageList(conversationId, cursor, idAfter, limit,
        sortDirection, sortBy);
    return ResponseEntity.status(HttpStatus.OK).body(responseDto);

  }

  @GetMapping("/with")
  public ResponseEntity<DirectMessageWithDto> getDirectMessageWith(@RequestParam UUID userId) {
    DirectMessageWithDto directMessageWithDto = service.getDirectMessageWith(userId);
    return ResponseEntity.status(HttpStatus.OK).body(directMessageWithDto);
  }

}
