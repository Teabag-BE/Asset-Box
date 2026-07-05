package io.teabag.assetbox.post.dto;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;

public record PostViewerFileResponse(
    String originalName,
    String accessUrl,
    AssetFileType fileType
) {
    public static PostViewerFileResponse from(FileAttachmentResponse file) {
        return new PostViewerFileResponse(
            file.originalName(),
            file.accessUrl(),
            file.fileType()
        );
    }
}
