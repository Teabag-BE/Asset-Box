package io.teabag.assetbox.file.controller;

import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.FileUploadRequest;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.file.service.download.FileDownloadService;
import io.teabag.assetbox.file.service.upload.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileDownloadService fileDownloadService;
    private final FileUploadService fileUploadService;

    //파일 업로드
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
        @RequestPart("files") List<MultipartFile> files,
        @RequestPart("infos") FileUploadRequest request) {
        FileUploadResponse response = fileUploadService.uploadFiles(files, request);
        return new ResponseEntity<>(ApiResponse.created(response, "파일 저장에 성공했습니다"), HttpStatus.CREATED);

    }

    //파일 썸네일 업로드
    @PostMapping("/upload/thumbnail")
    public ResponseEntity<ApiResponse<String>> uploadThumbnail(@RequestPart("file") MultipartFile file){
        String url = fileUploadService.uploadThumbnail(file, ThumbnailPurpose.POST, 1L);
        return new ResponseEntity<>(ApiResponse.created(url, "파일 저장에 성공했습니다"), HttpStatus.CREATED);
    }

    // 파일을 다운로드 하는 Presigned url 발급
    @GetMapping("/download/presigned-url")
    public ResponseEntity<ApiResponse<String>> getDownloadPresignedUrl(@RequestParam String fileName){

        String presignedUrl = fileDownloadService.getDownloadPresignedUrl(fileName);
        return new ResponseEntity<>(ApiResponse.ok(presignedUrl, SuccessCode.FILE_ISSUE_PRESIGNED_URL.getSuccessMessage()), HttpStatus.OK);
    }
}
