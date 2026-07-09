package com.sleepyproject.sleepy_backend.api.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${upload.path:uploads/}") // application.properties에서 설정, 기본값 uploads/
    private String uploadDir;

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "파일이 없습니다."));
        }

        try {
            // 업로드 디렉토리 생성
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 원본 파일명에서 확장자 추출
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 고유한 파일명 생성 (UUID)
            String savedFilename = UUID.randomUUID().toString() + extension;
            Path filePath = Paths.get(uploadDir, savedFilename);

            // 파일 저장
            Files.write(filePath, file.getBytes());

            // 클라이언트가 접근할 수 있는 URL 반환
            // 서버 호스트/포트가 클라이언트에서 접근 가능해야 하므로 보통 상대경로 또는 설정된 호스트를 반환
            String fileUrl = "http://localhost:8383/uploads/" + savedFilename;

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "파일 업로드 중 오류가 발생했습니다."));
        }
    }
}
