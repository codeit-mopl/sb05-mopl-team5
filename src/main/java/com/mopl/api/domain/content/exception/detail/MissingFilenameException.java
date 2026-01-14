package com.mopl.api.domain.content.exception.detail;

import com.mopl.api.domain.content.exception.ContentErrorCode;
import com.mopl.api.domain.content.exception.ContentErrorException;

public class MissingFilenameException extends ContentErrorException {

    public MissingFilenameException() {
        super(ContentErrorCode.MISSING_FILENAME);
    }

    public static MissingFilenameException WithFilename(String filename) {
        MissingFilenameException exception = new MissingFilenameException();
        exception.addDetail("filename", filename);
        return exception;
    }
}
