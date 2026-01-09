package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.dm.dto.response.conversation.ConversationListRow;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConversationRepositoryCustom {

    // 검색, 정렬, 커서 페이징이 포함된 목록 조회
    List<ConversationListRow> findConversationList(
        UUID myId,
        String keywordLike,
        LocalDateTime cursorTime,
        UUID idAfter,
        int limit,
        String sortDirection
    );
}