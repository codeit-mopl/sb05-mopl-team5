package com.mopl.api.global.config.exception;

import com.mopl.api.domain.conversation.exception.ConversationNotFoundException;
import com.mopl.api.global.config.ConversationNotFoundErrorResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation error: {}", e.getMessage());
        Map<String, String> errors = Map.of(
            "error", "Validation failed",
            "message", e.getBindingResult().getAllErrors().get(0).getDefaultMessage()
        );
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.badRequest()
                             .build();
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ConversationNotFoundErrorResponse> handleConversationNotFound(ConversationNotFoundException e) {

        Map<String, String> details = Map.of("withUserId", e.getWithUserId().toString());

        ConversationNotFoundErrorResponse response = new ConversationNotFoundErrorResponse(
            "conversation.not_found",
            e.getMessage(),
            details
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

}