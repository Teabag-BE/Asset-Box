package io.teabag.assetbox.file.controller;

import io.teabag.assetbox.file.service.download.FileDownloadService;
import io.teabag.assetbox.file.service.upload.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileDownloadService fileDownloadService;
    private final FileUploadService fileUploadService;


}
