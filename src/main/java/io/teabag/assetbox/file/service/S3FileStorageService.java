package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${custom.s3.bucket-name}")
    private String bucket;


    /*file upload*/
    public void upload(MultipartFile file, String s3key) {
        try{
            PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3key)
                .contentType(file.getContentType())
                .build();

            s3Client.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Uploaded file {} to bucket {}", file.getOriginalFilename(), bucket);
        } catch (Exception e) {
            log.warn("Failed to upload file to S3", e);
            throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED);
        }
    }

    public String uploadWithUrl(MultipartFile file, String s3key) {
        upload(file, s3key);
        return createShowPresignedUrl(s3key);
    }


    /*file download*/
    public String createDownloadPresignedUrl(String fileName) {
        // 다운로드할 객체 지정 (확장자 포함)
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key("test/"+fileName)
                // 보여주기가 아닌 다운로드로 강제 -> header에 추가
                .responseContentDisposition(
                        "attachment; filename=\""+fileName+"\""
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

    public String createShowPresignedUrl(String fileName) {
        // 다운로드할 객체 지정 (확장자 포함)
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        //Presigned URL 받아오기
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(120))  //2시간 제한
                        .getObjectRequest(objectRequest)
                        .build());
        log.info("Show Presigned URL: {}",presignedRequest.url().toString());

        // Presigned url 반환
        return presignedRequest.url().toString();
    }
}
