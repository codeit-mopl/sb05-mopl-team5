package com.mopl.api.domain.content.exception.detail;

import com.mopl.api.domain.content.exception.ContentErrorCode;
import com.mopl.api.domain.content.exception.ContentErrorException;
import java.util.UUID;

public class ContentNotFoundException extends ContentErrorException {

    public ContentNotFoundException() {
        super(ContentErrorCode.CONTENT_NOT_FOUND);
    }

    public static ContentNotFoundException withContentId(UUID contentId) {
        ContentNotFoundException exception = new ContentNotFoundException();
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
