package com.sleepyproject.sleepy_backend.api.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "general") String type) {
            
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "파일이 없습니다."));
        }

        try {
            // 원본 파일명에서 확장자 추출
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 업로드 타입에 따라 S3 물리 폴더 경로(Key Prefix) 분기 처리
            String folderPrefix;
            switch (type) {
                case "product-main":
                    folderPrefix = "products/main/";
                    break;
                case "product-detail":
                    folderPrefix = "products/detail/";
                    break;
                case "product-video":
                    folderPrefix = "products/video/";
                    break;
                case "post":
                    folderPrefix = "community/posts/";
                    break;
                default:
                    folderPrefix = "general/";
                    break;
            }

            // 고유한 파일명 생성 (S3 Key = 폴더 경로 + UUID파일명)
            String savedFilename = folderPrefix + UUID.randomUUID().toString() + extension;

            // Content-Type 결정 (기본값 application/octet-stream)
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // S3 업로드 요청 객체 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(savedFilename)
                    .contentType(contentType)
                    .build();

            // S3에 스트림으로 파일 업로드
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 업로드 완료된 S3 파일의 공개 URL 주소 생성
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, savedFilename);

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "S3 파일 업로드 중 오류가 발생했습니다."));
        }
    }
}
