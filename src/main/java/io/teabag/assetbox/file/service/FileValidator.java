package io.teabag.assetbox.file.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;

// 파일 업로드 전 확장자, 크기 등에 대한 검증
@Component
public class FileValidator {
	private static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L;
	private static final long MAX_TOTAL_FILE_SIZE_BYTES = 20 * 1024 * 1024L;
	private static final long MAX_THUMBNAIL_SIZE_BYTES = 5 * 1024 * 1024L;


	// 상태를 갖는 빈
	// 싱글톤 때문
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
		"fbx",
		"png",
		"jpg",
		"jpeg"
		// "obj",
		// "glb"
	);

	private static final Set<String> THUMBNAIL_ALLOWED_EXTENSIONS = Set.of(
			"png",
			"jpg",
			"jpeg"
	);

	public static final String MODEL_ALLOWED_EXTENSION = "fbx";


	/*
	현재 게시글 용량 + 새로 추가할 파일들 용량.
	 */
	public void validateFilesTotalSize(long currentTotalSizeBytes, List<MultipartFile> newFiles) {
		long newUploadSizeBytes = newFiles.stream()
			.mapToLong(MultipartFile::getSize)
			.sum();

		long totalSizeBytes = currentTotalSizeBytes + newUploadSizeBytes;

		if (totalSizeBytes > MAX_TOTAL_FILE_SIZE_BYTES) {
			throw new BusinessException(ErrorCode.FILE_TOTAL_SIZE_INVALID);
		}
	}

	public void validateThumbnail(MultipartFile file){
		validateNotEmpty(file);
		validateThumbnailSize(file);
		validateImageExtension(file);
	}

	public void validateImageExtension(MultipartFile file) {
		String extension = extractExtension(file.getOriginalFilename());

		if (!THUMBNAIL_ALLOWED_EXTENSIONS.contains(extension)) {
			throw new BusinessException(ErrorCode.EXTENSIONS_INVALID);
		}
	}

	public void validate(MultipartFile file){
		validateNotEmpty(file);
		validateSize(file);
		validateExtension(file.getOriginalFilename());
	}

	private void validateNotEmpty(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.FILE_EMPTY);
		}
	}

	private void validateSize(MultipartFile file) {
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new BusinessException(ErrorCode.SIZE_INVALID);
		}
	}
	void validateThumbnailSize(MultipartFile file) {
		if (file.getSize() > MAX_THUMBNAIL_SIZE_BYTES) {
			throw new BusinessException(ErrorCode.THUMBNAIL_SIZE_INVALID);
		}
	}

	private void validateExtension(String originalFilename) {
		String extension = extractExtension(originalFilename);

		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new BusinessException(ErrorCode.EXTENSIONS_INVALID);
		}
	}

	public String extractExtension(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			throw new BusinessException(ErrorCode.FILE_NAME_EMPTY);
		}

		int dotIndex = originalFilename.lastIndexOf(".");

		if (dotIndex == -1 || dotIndex == originalFilename.length() - 1) {
			throw new BusinessException(ErrorCode.EXTENSIONS_INVALID);
		}

		return originalFilename.substring(dotIndex + 1).toLowerCase();
	}

	public boolean validateModel(String extension) {
		return extension.equals("fbx");
	}
}
