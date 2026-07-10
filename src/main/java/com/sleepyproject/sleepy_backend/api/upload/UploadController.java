package com.sleepyproject.sleepy_backend.api.upload;

import com.sleepyproject.sleepy_backend.service.upload.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 파일 업로드 관련 HTTP 요청을 수신하는 컨트롤러 클래스입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    /**
     * 클라이언트로부터 전달받은 단일 파일을 AWS S3 저장소에 업로드합니다.
     *
     * @param file 업로드할 파일 객체 (MultipartFile)
     * @param type 파일 저장 분류 유형 (기본값: general)
     * @return 업로드된 파일의 S3 접근 URL 주소
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "general") String type) {

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "파일이 없습니다."));
        }

        try {
            String fileUrl = uploadService.uploadFile(file, type);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            log.error("S3 파일 업로드 처리 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "S3 파일 업로드 중 오류가 발생했습니다."));
        }
    }
}
