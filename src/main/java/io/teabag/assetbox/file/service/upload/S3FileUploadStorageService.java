package io.teabag.assetbox.file.service.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileUploadStorageService {

    private final S3Client s3Client;

    @Value("${custom.s3.bucket-name}")
    private String bucket;



    public void upload(MultipartFile file, String s3key) {
        try{
            PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3key)
                .contentType(file.getContentType())
                .build();

            s3Client.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            log.warn("Failed to upload file to S3", e);
            throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED);
        }
    }
}
