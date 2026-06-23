package io.teabag.assetbox.file.service.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


import java.time.LocalDate;
import java.util.UUID;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.service.FileValidator;
import io.teabag.assetbox.file.service.S3FileKeyGenerator;
import org.junit.jupiter.api.Test;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;

public class S3FileKeyGeneratorTest {

	private FileValidator validator =  new FileValidator();
	private final S3FileKeyGenerator generator = new S3FileKeyGenerator(validator);

	@Test
	void file_원본_파일로_S3_key를_생성한다(){
		//given
		FilePurpose purpose = FilePurpose.ASSET;
		Long purposeId = 1L;
		AssetFileType fileType = AssetFileType.MODEL;
		UUID uploadBatchId = UUID.fromString("9c54f9e1-0c2a-43cb-a70f-97b9a0b3b123");
		String originalFilename = "tree.FBX";

		LocalDate today = LocalDate.now();
		String expectedPrefix = "assets/asset/1/model/%d/%02d/%02d/%s/".formatted(
			today.getYear(),
			today.getMonthValue(),
			today.getDayOfMonth(),
			uploadBatchId
		);

		//when
		String s3Key = generator.generate(
			purpose,
			purposeId,
			fileType,
			uploadBatchId,
			originalFilename
		);


		//then
		assertThat(s3Key).startsWith(expectedPrefix);
		assertThat(s3Key).endsWith(".fbx");
		assertThat(s3Key).doesNotContain("tree.FBX");

	}

	@Test
	void file_확장자가_없으면_S3Key_생성할때_예외발생(){
		// given
		FilePurpose purpose = FilePurpose.ASSET;
		Long purposeId = 1L;
		AssetFileType fileType = AssetFileType.MODEL;
		UUID uploadBatchId = UUID.randomUUID();
		String originFilename = "tree";

		assertThatThrownBy(()->generator.generate(
			purpose,
			purposeId,
			fileType,
			uploadBatchId,
			originFilename
		))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("허용되지 않은 확장자입니다.");
	}

}
