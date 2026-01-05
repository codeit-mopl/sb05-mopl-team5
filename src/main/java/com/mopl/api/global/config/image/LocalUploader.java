package com.mopl.api.global.config.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalUploader {

    private final String UPLOAD_DIR = "src/main/resources/static/contents";

    public String upload(MultipartFile file) {
        try {
            File dir = new File(UPLOAD_DIR);

            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = UUID.randomUUID() + file.getOriginalFilename();

            Path path = Paths.get(UPLOAD_DIR + "/" + fileName);

            Files.copy(file.getInputStream(), path);

            return "/contents/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
