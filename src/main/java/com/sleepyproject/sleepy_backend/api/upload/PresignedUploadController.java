package com.sleepyproject.sleepy_backend.api.upload;

import com.sleepyproject.sleepy_backend.service.upload.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * S3 Presigned URL 발급 API 컨트롤러.
 * 클라이언트(앱)가 서버를 거치지 않고 S3에 직접 파일을 업로드할 수 있도록
 * 임시 서명된 URL을 발급합니다.
 *
 * <p>업로드 흐름:
 * <ol>
 *   <li>앱 → GET /api/upload/presigned-url 으로 URL 발급 요청</li>
 *   <li>서버 → presignedUrl + fileUrl 반환 (15분 유효)</li>
 *   <li>앱 → presignedUrl로 S3에 직접 PUT 업로드</li>
 *   <li>앱 → fileUrl을 게시글 imageUrl 필드에 포함하여 게시글 작성</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class PresignedUploadController {

    private final UploadService uploadService;

    /**
     * Presigned PUT URL을 발급합니다.
     *
     * @param fileName    클라이언트의 원본 파일명 (확장자 추출에 사용)
     * @param contentType 파일의 MIME 타입 (예: video/mp4, image/jpeg)
     * @param type        업로드 목적 유형 (예: post, product-main, product-video)
     * @return presignedUrl (S3 직접 업로드용), fileUrl (업로드 후 사용할 S3 공개 URL)
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @RequestParam("fileName") String fileName,
            @RequestParam("contentType") String contentType,
            @RequestParam(value = "type", defaultValue = "post") String type) {

        if (fileName == null || fileName.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "fileName 파라미터가 필요합니다."));
        }

        try {
            Map<String, String> result = uploadService.generatePresignedUrl(fileName, contentType, type);
            log.info("Presigned URL 발급 완료 - fileName: {}, type: {}", fileName, type);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Presigned URL 발급 거부 - 사유: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Presigned URL 발급 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Presigned URL 발급 중 오류가 발생했습니다."));
        }
    }
}
