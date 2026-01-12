package com.mopl.api.domain.content.repository.impl;

import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.dto.response.CursorResponseContentDto;

public interface ContentRepositoryCustom {

    CursorResponseContentDto findContentsByCursor(ContentSearchRequest request);
}
