package com.mopl.api.global.config.image;

import org.springframework.web.multipart.MultipartFile;

public interface Uploader {
    String upload(MultipartFile file);
}
