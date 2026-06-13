package io.teabag.assetbox.file.dto;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;

public record FilePreviewResponse(
	Long fileId,
	String originalName,
	String s3Key,
	String previewUrl,
	Long sizeBytes,
	AssetFileType fileType,
	Long uploadOrder
) {
	public static FilePreviewResponse from(File file, String previewUrl) {
		return new FilePreviewResponse(
			file.getId(),
			file.getOriginalName(),
			file.getS3Key(),
			previewUrl,
			file.getSizeBytes(),
			file.getFileType(),
			file.getUploadOrder()
		);
	}
}