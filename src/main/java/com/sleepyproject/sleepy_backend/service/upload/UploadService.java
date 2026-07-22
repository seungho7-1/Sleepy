package com.sleepyproject.sleepy_backend.service.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * AWS S3 파일 업로드를 처리하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

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

        String folderPrefix = resolveFolderPrefix(type);

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

    /**
     * 클라이언트가 S3에 직접 파일을 업로드할 수 있도록 Presigned PUT URL을 생성합니다.
     * 이 방식을 사용하면 파일이 서버를 거치지 않고 S3로 직접 전송되어
     * Nginx 업로드 제한을 우회하고 서버 부하를 줄일 수 있습니다.
     *
     * @param originalFilename 원본 파일명 (확장자 추출에 사용)
     * @param contentType      파일의 MIME 타입 (예: video/mp4, image/jpeg)
     * @param type             업로드 목적 유형 (예: post, product-main 등)
     * @return Presigned URL과 업로드 완료 후 사용할 S3 파일 URL을 담은 Map
     */
    public java.util.Map<String, String> generatePresignedUrl(String originalFilename, String contentType, String type) {
        // 확장자 추출
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // 보안: 허용된 확장자만 Presigned URL 발급
        java.util.List<String> allowedExtensions = java.util.List.of(
                ".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic", ".heif",
                ".mp4", ".webm", ".mov", ".avi", ".mkv", ".m4v"
        );
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다: " + extension);
        }

        String folderPrefix = resolveFolderPrefix(type);
        String s3Key = folderPrefix + UUID.randomUUID().toString() + extension;

        // Content-Type 결정
        String resolvedContentType = (contentType != null && !contentType.isBlank())
                ? contentType
                : "application/octet-stream";

        // Presigned PUT URL 생성 (유효시간: 15분)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(resolvedContentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedRequest.url().toString();
        String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);

        log.info("Presigned URL 발급 - S3 Key: {}, ContentType: {}, 만료: 15분", s3Key, resolvedContentType);

        return java.util.Map.of(
                "presignedUrl", presignedUrl,
                "fileUrl", fileUrl
        );
    }

    /**
     * 업로드 타입 문자열에 따라 S3 폴더 경로(Key Prefix)를 결정합니다.
     *
     * @param type 업로드 목적 유형
     * @return S3 폴더 경로 문자열
     */
    private String resolveFolderPrefix(String type) {
        return switch (type) {
            case "product-main"   -> "products/main/";
            case "product-detail" -> "products/detail/";
            case "product-video"  -> "products/video/";
            case "post"           -> "community/posts/";
            default               -> "general/";
        };
    }
}

