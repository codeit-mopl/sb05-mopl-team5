package com.mopl.api.domain.content.exception.detail;

import com.mopl.api.domain.content.exception.ContentErrorCode;
import com.mopl.api.domain.content.exception.ContentErrorException;

public class InvalidSortByException extends ContentErrorException {

    public InvalidSortByException() {
        super(ContentErrorCode.INVALID_SORT_BY);
    }

    public static InvalidSortByException withSortBy(String sortBy) {
        InvalidSortByException exception = new InvalidSortByException();
        exception.addDetail("sortBy", sortBy);
        return exception;
    }
}
