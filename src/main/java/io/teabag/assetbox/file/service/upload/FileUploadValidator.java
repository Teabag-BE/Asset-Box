package io.teabag.assetbox.file.service.upload;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// 파일 업로드 전 확장자, 크기 등에 대한 검증
@Component
public class FileUploadValidator {
	private static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L;

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
		"fbx",
		"png",
		"jpg",
		"jpeg"
		// "obj",
		// "glb"
	);
	public void validate(MultipartFile file){
		validateNotEmpty(file);
		validateSize(file);
		validateExtension(file.getOriginalFilename());
	}

	private void validateNotEmpty(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("파일이 비어 있습니다.");
		}
	}

	private void validateSize(MultipartFile file) {
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new IllegalArgumentException("파일 크기는 20MB를 초과할 수 없습니다.");
		}
	}

	private void validateExtension(String originalFilename) {
		String extension = extractExtension(originalFilename);

		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException("허용되지 않은 파일 형식입니다. extension=" + extension);
		}
	}

	public String extractExtension(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			throw new IllegalArgumentException("파일명이 비어 있습니다.");
		}

		int dotIndex = originalFilename.lastIndexOf(".");

		if (dotIndex == -1 || dotIndex == originalFilename.length() - 1) {
			throw new IllegalArgumentException("파일 확장자가 없습니다.");
		}

		return originalFilename.substring(dotIndex + 1).toLowerCase();
	}
}
