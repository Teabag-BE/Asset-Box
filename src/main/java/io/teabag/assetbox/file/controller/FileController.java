package io.teabag.assetbox.file.controller;

import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.FileUpdateRequest;
import io.teabag.assetbox.file.dto.FileUploadRequest;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final UserService userService;

    //파일 업로드
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
        @RequestPart("files") List<MultipartFile> files,
        @RequestPart("infos") FileUploadRequest request) {
        FileUploadResponse response = fileService.uploadFiles(files, request);
        return new ResponseEntity<>(ApiResponse.created(response, "파일 저장에 성공했습니다"), HttpStatus.CREATED);

    }

    //파일 썸네일 업로드
    @PostMapping("/upload/thumbnail")
    public ResponseEntity<ApiResponse<String>> uploadThumbnail(@RequestPart("file") MultipartFile file){
        String url = fileService.uploadThumbnail(file, ThumbnailPurpose.POST, 1L);
        return new ResponseEntity<>(ApiResponse.created(url, "파일 저장에 성공했습니다"), HttpStatus.CREATED);
    }

    // 파일 미리보기 Presigned url 발급
    @GetMapping("/get/presigned-url")
    public ResponseEntity<ApiResponse<String>> getShowPresignedUrl(@RequestParam String s3Key){
        String presignedUrl = fileService.getShowPresignedUrl(s3Key);
        return new ResponseEntity<>(ApiResponse.ok(presignedUrl, SuccessCode.FILE_ISSUE_PRESIGNED_URL.getSuccessMessage()), HttpStatus.OK);
    }

    // 해당 도메인의 미리보기 Presigned url 발급
    @GetMapping("/get/presigned-urls")
    public ResponseEntity<ApiResponse<List<String>>> getShowPresignedUrl(@RequestParam String filePurpose, Long filePurposeId){
        List<String> presignedUrls = fileService.getShowPresignedUrlsByPurpose(filePurpose, filePurposeId);
        return new ResponseEntity<>(ApiResponse.ok(presignedUrls, SuccessCode.FILE_ISSUE_PRESIGNED_URL.getSuccessMessage()), HttpStatus.OK);
    }

    // 파일을 다운로드 하는 Presigned url 발급
    @GetMapping("/download/presigned-url")
    public ResponseEntity<ApiResponse<String>> getDownloadPresignedUrl(@RequestParam(required = false) Long fileId){

        String presignedUrl = fileService.getDownloadPresignedUrl(fileId);
        return new ResponseEntity<>(ApiResponse.ok(presignedUrl, SuccessCode.FILE_ISSUE_PRESIGNED_URL.getSuccessMessage()), HttpStatus.OK);
    }

    @GetMapping("/{fileId}/download-url")
    public ResponseEntity<ApiResponse<String>> getDownloadPresignedUrlByPath(@PathVariable Long fileId){
        String presignedUrl = fileService.getDownloadPresignedUrl(fileId);
        return new ResponseEntity<>(ApiResponse.ok(presignedUrl, SuccessCode.FILE_ISSUE_PRESIGNED_URL.getSuccessMessage()), HttpStatus.OK);
    }

    //파일 수정
    @PatchMapping
    public ResponseEntity<ApiResponse<FileUploadResponse>> updateFile(@RequestPart("files") List<MultipartFile> files,
                                                                      @RequestPart("request") FileUpdateRequest request,
                                                                      @AuthenticationPrincipal CurrentUser currentUser){
        FileUploadResponse response = fileService.updateFilesTest(files, request, FilePurpose.REQUEST_REFERENCE, 1L, currentUser);
        return new ResponseEntity<>(ApiResponse.created(response, "파일 저장에 성공했습니다"), HttpStatus.CREATED);
    }
}
