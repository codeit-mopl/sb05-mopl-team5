package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.dto.request.DirectMessageSendRequest;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import java.util.UUID;

public interface DirectMessageCommandService {

     DirectMessageDto send(UUID conversationId, UUID senderId, DirectMessageSendRequest request);


}
