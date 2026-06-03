package io.teabag.assetbox.file.service.download;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileDownloadStorageService {

//    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${custom.s3.bucket-name}")
    private String bucket;

    public String createPresignedUrl(String fileName) {
        // 다운로드할 객체 지정 (확장자 포함)
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key("test/"+fileName)
                // 보여주기가 아닌 다운로드로 강제 -> header에 추가
                .responseContentDisposition(
                        "attachment; filename=\"Mybatis2.md\""
                )
                .build();

        //Presigned URL 받아오기
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))  //10분 제한
                        .getObjectRequest(objectRequest)
                        .build());
        log.info("Presigned URL: {}",presignedRequest.url().toString());

        // Presigned url 반환
        return presignedRequest.url().toString();
    }




}
