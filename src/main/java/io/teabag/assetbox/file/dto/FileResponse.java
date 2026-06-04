package io.teabag.assetbox.file.dto;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;

public record FileResponse(
    Long id,
    String originalName,
    String savedName,
    String extension,
    Long sizeBytes,
    FilePurpose purpose,
    Long purposeId,
    AssetFileType fileType,
    String uploadBatchId,
    Long uploadOrder
){
    public static FileResponse from(File file) {
        return new FileResponse(
            file.getId(),
            file.getOriginalName(),
            file.getSavedName(),
            file.getExtension(),
            file.getSizeBytes(),
            file.getPurpose(),
            file.getPurposeId(),
            file.getFileType(),
            file.getUploadBatchId(),
            file.getUploadOrder()
        );
    }
}
