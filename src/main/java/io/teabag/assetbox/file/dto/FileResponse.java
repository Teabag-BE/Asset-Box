package io.teabag.assetbox.file.dto;

import io.teabag.assetbox.file.domain.AssetFile;
import io.teabag.assetbox.file.domain.FilePurpose;

public record FileResponse(Long id, String originalName, String extension, long sizeBytes, FilePurpose purpose, Long ownerId, Long uploadedBy) {
    public static FileResponse from(AssetFile file) {
        return new FileResponse(file.getId(), file.getOriginalName(), file.getExtension(), file.getSizeBytes(), file.getPurpose(), file.getOwnerId(), file.getUploadedBy());
    }
}
