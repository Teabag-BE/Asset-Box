package io.teabag.assetbox.file.domain;

import io.teabag.assetbox.file.service.FileValidator;

public enum AssetFileType {
	MODEL,
	TEXTURE,
	REFERENCE,
	ETC;

	public  static AssetFileType fromFile(FilePurpose purpose, String extension) {
		if (purpose == FilePurpose.ASSET) {
			if (extension.equals(FileValidator.MODEL_ALLOWED_EXTENSION)) return AssetFileType.MODEL;
			else return  AssetFileType.TEXTURE;
		}
		else if (purpose == FilePurpose.REQUEST_REFERENCE) return AssetFileType.REFERENCE;
		else return AssetFileType.ETC;
	}
}
