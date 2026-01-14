package com.mopl.api.global.config;

import java.util.Map;

public record ConversationNotFoundErrorResponse (
    String exceptionName,
    String message,
    Map<String, String> details
){

}
