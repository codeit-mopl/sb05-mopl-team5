package com.mopl.api.domain.user.service;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileImageStorageService {


    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private final Path root = Paths.get("uploads", "profile-images");


    public String store(UUID userId, MultipartFile file) {
        validate(file);

        try {
            Files.createDirectories(root);

            String ext = extension(file.getOriginalFilename());
            String filename = userId + "_" + UUID.randomUUID() + ext;

            Path dest = root.resolve(filename).normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            // 외부로 노출할 URL 규칙 (리버스 프록시/정적 리소스 매핑과 맞추기)
            return "/profile-images/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("프로필 이미지 저장에 실패했습니다.", e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("이미지는 최대 5MB까지 업로드할 수 있습니다.");
        }
        String ct = file.getContentType();
        if (ct == null || (!ALLOWED.contains(ct) && !ct.startsWith("image/"))) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        // 실제 파일 내용이 이미지인지 검증
        try (InputStream is = file.getInputStream()) {
            if (ImageIO.read(is) == null) {
                throw new IllegalArgumentException("유효한 이미지 파일이 아닙니다.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지 파일 검증에 실패했습니다.", e);
        }
    }

    private String extension(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

}
