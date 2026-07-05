package io.teabag.assetbox.post.dto;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;

public record PostDownloadFileResponse(
        Long fileId,
        String originalName,
        String extension,
        Long sizeBytes,
        AssetFileType fileType
) {
    public static PostDownloadFileResponse from(FileAttachmentResponse file) {
        return new PostDownloadFileResponse(
                file.fileId(),
                file.originalName(),
                file.extension(),
                file.sizeBytes(),
                file.fileType()
        );
    }
}
