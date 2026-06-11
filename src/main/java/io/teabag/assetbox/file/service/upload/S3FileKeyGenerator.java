package io.teabag.assetbox.file.service.upload;

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

	private final FileUploadValidator fileUploadValidator;

	public String generate(
		FilePurpose purpose,
		Long purposeId,
		AssetFileType fileType,
		UUID uploadBatchId,
		String originalFilename
	){
		String extension = fileUploadValidator.extractExtension(originalFilename);
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
		String extension = fileUploadValidator.extractExtension(originalFilename);
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

}
