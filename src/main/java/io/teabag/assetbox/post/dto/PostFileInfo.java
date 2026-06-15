package io.teabag.assetbox.post.dto;

import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileResponse;

public record PostFileInfo(Long id,
                           String originalName,
                           String extension,
                           Long sizeBytes) {
    public static PostFileInfo from(FileResponse fileInfo) {
        return new PostFileInfo(fileInfo.id(), fileInfo.originalName(), fileInfo.extension(), fileInfo.sizeBytes());
    }
    public static PostFileInfo from(FileAttachmentResponse fileInfo) {
        return new PostFileInfo(fileInfo.fileId(), fileInfo.originalName(), fileInfo.extension(), fileInfo.sizeBytes());
    }
}
