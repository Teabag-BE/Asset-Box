package io.teabag.assetbox.file.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.file.dto.FileResponse;
import io.teabag.assetbox.file.service.FileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/{fileId}/meta")
    public ApiResponse<FileResponse> meta(@PathVariable Long fileId) {
        return ApiResponse.ok(fileService.meta(fileId));
    }
}
