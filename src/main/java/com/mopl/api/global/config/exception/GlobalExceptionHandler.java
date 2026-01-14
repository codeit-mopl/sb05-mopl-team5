package com.mopl.api.global.config.exception;

import com.mopl.api.domain.conversation.exception.ConversationNotFoundException;
import com.mopl.api.global.config.ConversationNotFoundErrorResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleException(Exception e) {
        log.error(e.getMessage());
        return ResponseEntity.badRequest()
                             .build();
    }
    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ConversationNotFoundErrorResponse> handleConversationNotFound(ConversationNotFoundException e) {

        // 프론트가 원하는 "details" 객체 만들기
        Map<String, String> details = Map.of("withUserId", e.getWithUserId().toString());

        // 프론트가 원하는 JSON 구조로 응답 생성
        ConversationNotFoundErrorResponse response = new ConversationNotFoundErrorResponse(
            "conversation.not_found", // 프론트가 식별하는 에러 코드
            e.getMessage(),           // "대화를 찾을 수 없습니다."
            details                   // { "withUserId": "..." }
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

}