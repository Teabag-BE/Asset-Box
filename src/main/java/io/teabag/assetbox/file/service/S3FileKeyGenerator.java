package io.teabag.assetbox.file.service;

// 사용자가 올린 원본 파일명을 그대로 쓰지 않고,
// S3에 저장할 안전한 고유 경로인 Key를 만들어주는 클래스

import java.time.LocalDate;
import java.util.UUID;

import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import org.springframework.stereotype.Component;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class S3FileKeyGenerator {

	private final FileValidator fileValidator;

	public String generate(
		FilePurpose purpose,
		Long purposeId,
		AssetFileType fileType,
		UUID uploadBatchId,
		String originalFilename
	){
		String extension = fileValidator.extractExtension(originalFilename);
		LocalDate now = LocalDate.now();
		String fileUuid = UUID.randomUUID().toString();

		return "assets/%s/%d/%s/%d/%02d/%02d/%s/%s.%s".formatted(
			purpose.name().toLowerCase(),
			purposeId,
			fileType.name().toLowerCase(),
			now.getYear(),
			now.getMonthValue(),
			now.getDayOfMonth(),
			uploadBatchId,
			fileUuid,
			extension
		);
	}

	public String generateThumbnail(
			ThumbnailPurpose purpose,
			Long purposeId,
			String originalFilename
	){
		String extension = fileValidator.extractExtension(originalFilename);
		LocalDate now = LocalDate.now();
		String fileUuid = UUID.randomUUID().toString();
		String uploadBatchId = UUID.randomUUID().toString();

		return "assets/%s/%d/%s/%d/%02d/%02d/%s/%s.%s".formatted(
				purpose.name().toLowerCase(),
				purposeId,
				"thumbnail",
				now.getYear(),
				now.getMonthValue(),
				now.getDayOfMonth(),
				uploadBatchId,
				fileUuid,
				extension
		);
	}

	public String generatePostOriginalZip(Long postId, UUID uploadBatchId) {
		return "posts/%d/original/%s.zip".formatted(postId, uploadBatchId);
	}

	public String generatePostViewerModel(Long postId) {
		return generatePostViewerModel(postId, FileValidator.MODEL_ALLOWED_EXTENSION);
	}

	public String generatePostViewerModel(Long postId, String extension) {
		return "posts/%d/viewer/model/%s.%s".formatted(postId, UUID.randomUUID(), extension);
	}

	public String generatePostViewerTexture(Long postId, String originalFilename) {
		String safePath = sanitizeRelativePath(originalFilename);
		int slashIndex = safePath.lastIndexOf('/');
		String directory = slashIndex >= 0 ? safePath.substring(0, slashIndex + 1) : "";
		String fileName = slashIndex >= 0 ? safePath.substring(slashIndex + 1) : safePath;
		return "posts/%d/viewer/textures/%s%s_%s".formatted(postId, directory, UUID.randomUUID(), fileName);
	}

	private String sanitizeRelativePath(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return "texture";
		}

		String normalized = originalFilename.replace('\\', '/');
		return java.util.Arrays.stream(normalized.split("/"))
			.filter(segment -> !segment.isBlank() && !segment.equals(".") && !segment.equals(".."))
			.map(segment -> segment.replaceAll("[^A-Za-z0-9._-]", "_"))
			.reduce((left, right) -> left + "/" + right)
			.orElse("texture");
	}

}
