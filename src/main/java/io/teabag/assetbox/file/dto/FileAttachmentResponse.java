package io.teabag.assetbox.file.dto;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;

public record FileAttachmentResponse(
	Long fileId,
	String originalName,
	String extension,
	String s3Key,
	String accessUrl,
	Long sizeBytes,
	AssetFileType fileType,
	Long uploadOrder
) {
	public static FileAttachmentResponse from(File file, String accessUrl) {
		return new FileAttachmentResponse(
			file.getId(),
			file.getOriginalName(),
			file.getExtension(),
			file.getS3Key(),
			accessUrl,
			file.getSizeBytes(),
			file.getFileType(),
			file.getUploadOrder()
		);
	}
}