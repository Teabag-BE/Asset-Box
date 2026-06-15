package io.teabag.assetbox.file.dto;

import io.teabag.assetbox.file.domain.AssetFileType;

import java.util.List;

public record AssetFileRequest(
        List<AssetFileType> assetTypes
) {
}
