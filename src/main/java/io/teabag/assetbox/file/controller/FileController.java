package io.teabag.assetbox.file.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.file.service.download.FileDownloadService;
import io.teabag.assetbox.file.service.upload.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileDownloadService fileDownloadService;
    private final FileUploadService fileUploadService;

    // 파일을 다운로드 하는 Presigned url 발급
    @GetMapping("/download")
    public ResponseEntity<ApiResponse<String>> getPresigned(@RequestParam String fileName){

        String presignedUrl = fileDownloadService.getPresignedDownloadUrl(fileName);
        return new ResponseEntity<>(ApiResponse.ok(presignedUrl), HttpStatus.OK);
    }


}
