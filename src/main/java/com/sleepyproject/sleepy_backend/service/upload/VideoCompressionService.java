package com.sleepyproject.sleepy_backend.service.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCompressionService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Async
    public void compressVideoAsync(String fileUrl) {
        if (fileUrl == null || (!fileUrl.toLowerCase().endsWith(".mp4") && !fileUrl.toLowerCase().endsWith(".mov") && !fileUrl.toLowerCase().endsWith(".webm"))) {
            return;
        }

        try {
            // Extract S3 Key from URL
            String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            if (!fileUrl.startsWith(prefix)) {
                log.warn("Invalid S3 URL for compression: {}", fileUrl);
                return;
            }
            String s3Key = fileUrl.substring(prefix.length());

            log.info("Starting async video compression for S3 Key: {}", s3Key);

            // 1. Download original file from S3 to temp local file
            Path tempInputPath = Files.createTempFile("video_input_", ".mp4");
            Path tempOutputPath = Files.createTempFile("video_output_", ".mp4");

            s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build(), tempInputPath);

            log.info("Downloaded original video to {}", tempInputPath.toAbsolutePath());

            File source = tempInputPath.toFile();
            File target = tempOutputPath.toFile();

            // Ignore files smaller than 5MB to avoid unnecessary processing
            if (source.length() < 5 * 1024 * 1024) {
                log.info("File size is small enough ({} bytes), skipping compression.", source.length());
                Files.deleteIfExists(tempInputPath);
                Files.deleteIfExists(tempOutputPath);
                return;
            }

            // 2. Compress video using Jave2
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("aac");
            audio.setBitRate(128000);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            VideoAttributes video = new VideoAttributes();
            video.setCodec("h264"); // Uses libx264 internally in Jave2
            // S3 5GB 무료 티어 생존을 위해 비트레이트를 1Mbps로 낮춤 (용량 대폭 절약)
            video.setBitRate(1000000); // 1.0 Mbps
            video.setFrameRate(30);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp4");
            attrs.setAudioAttributes(audio);
            attrs.setVideoAttributes(video);

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(source), target, attrs);

            log.info("Compression completed. Original size: {}, Compressed size: {}", source.length(), target.length());

            // 3. Upload compressed video back to S3, replacing original
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("video/mp4")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(target));
            log.info("Uploaded compressed video back to S3 Key: {}", s3Key);

            // 4. Cleanup temp files
            Files.deleteIfExists(tempInputPath);
            Files.deleteIfExists(tempOutputPath);

        } catch (Exception e) {
            log.error("Error during async video compression", e);
        }
    }
}
