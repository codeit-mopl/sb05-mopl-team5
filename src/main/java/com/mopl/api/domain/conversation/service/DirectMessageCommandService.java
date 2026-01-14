package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import java.util.UUID;

public interface DirectMessageCommandService {

    public DirectMessageDto send(UUID conversationId , UUID senderId, String content);


}
