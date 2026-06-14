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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

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
        try {
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

    //썸네일 저장시 s3key 반환
    public String uploadWiths3key(MultipartFile file, String s3key) {
        upload(file, s3key);
        return s3key;
    }

    /*file download*/
    public String createDownloadPresignedUrl(String s3Key, String originalName) {
        // 원본 이름 인코딩
        String encodedName  = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20");
        // 다운로드할 객체 지정 (확장자 포함)
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                // 보여주기가 아닌 다운로드로 강제 -> header에 추가
                .responseContentDisposition(
                        "attachment; filename=\"download\"; filename*=UTF-8''" + encodedName
                )
                .build();

        //Presigned URL 받아오기
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))  //10분 제한
                        .getObjectRequest(objectRequest)
                        .build());
        log.info("Create Download Presigned URL: {}",presignedRequest.url().toString());

        // Presigned url 반환
        return presignedRequest.url().toString();
    }

    // 파일 미리보기 presignedUrl 생성
    public String createShowPresignedUrl(String s3Key) {
        // 다운로드할 객체 지정 (확장자 포함)
        GetObjectRequest objectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .build();

        //Presigned URL 받아오기
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(120))  //2시간 제한
                        .getObjectRequest(objectRequest)
                        .build());
        log.info("Create Show Presigned URL: {}",presignedRequest.url().toString());

        // Presigned url 반환
        return presignedRequest.url().toString();
    }

    // 파일 삭제하기
    public void delete(String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Success to Delete File. bucket = {}, s3key = {} ", bucket, s3Key);
        } catch (Exception e) {
            log.warn("Failed to Delete File. s3key = {} ", s3Key, e);
            throw new BusinessException(ErrorCode.STORAGE_DELETE_FAILED);
        }
    }

    // 파일 다중 삭제하기
    public void deleteAll(List<String> s3Keys) {
        for (String s3Key : s3Keys) {
            delete(s3Key);
        }
    }
}
