package com.sleepyproject.sleepy_backend.service.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * AWS S3 파일 업로드를 처리하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * 파일을 AWS S3 버킷에 업로드하고 해당 파일의 퍼블릭 URL을 반환합니다.
     *
     * @param file 업로드할 멀티파트 파일
     * @param type 파일 업로드 목적/유형 (예: product-main, product-detail, product-video, post 등)
     * @return 업로드된 파일의 S3 퍼블릭 URL 경로
     * @throws IOException 파일 스트림 읽기 실패 또는 S3 연결 실패 시 발생
     */
    public String uploadFile(MultipartFile file, String type) throws IOException {
        // 원본 파일명에서 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // 보안: 이미지 및 비디오 파일 확장자 화이트리스트 검증 (아이폰 heic 추가)
        java.util.List<String> allowedExtensions = java.util.List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic", ".heif", ".mp4", ".webm", ".mov", ".avi");
        if (!allowedExtensions.contains(extension)) {
            // 프론트엔드 이미지 압축기가 확장자를 날려먹은 경우(blob 등) 강제로 .jpg 처리
            if (extension.isEmpty() || extension.equals(".blob")) {
                extension = ".jpg";
            } else {
                throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다: " + extension);
            }
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

        log.info("S3 파일 업로드 요청 - 원본명: {}, 저장경로: {}, Content-Type: {}", originalFilename, savedFilename, contentType);

        // S3 업로드 요청 객체 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(savedFilename)
                .contentType(contentType)
                .build();

        // S3에 스트림으로 파일 업로드
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // 업로드 완료된 S3 파일의 공개 URL 주소 생성 및 반환
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, savedFilename);
    }
}
