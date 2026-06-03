package io.teabag.assetbox.file.service.upload;

import org.springframework.web.multipart.MultipartFile;

public class FileUploadValidator {

	public void validate(MultipartFile file){
	}

	public String extractExtension(String originalFileName){
		return originalFileName;
	}
}
