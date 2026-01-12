package com.mopl.api.domain.content.repository.impl;

import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.entity.Content;
import java.util.List;

public interface ContentRepositoryCustom {

    List<Content> findContentsByCursor(ContentSearchRequest request);

    Long countContents(ContentSearchRequest request);
}
