package io.teabag.assetbox.file.service.upload;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.teabag.assetbox.file.domain.FilePurpose;

public class S3FileKeyGeneratorTest {

	private FileUploadValidator validator =  new FileUploadValidator();
	private final S3FileKeyGenerator generator = new S3FileKeyGenerator();

	@Test
	void file_원본_파일명으로_S3_key를_생성한다(){
		//given
		FilePurpose purpose = FilePurpose.ASSET;
		Long domainId = 1L;
		String originalFilename = "tree.FBX";

		//when
		String s3Key = generator.generate(purpose, domainId, originalFilename);

		//then
		assertThat(s3Key).startsWith("assets/post/1/");
		assertThat(s3Key).endsWith(".fbx");
		assertThat(s3Key).doesNotContain("tree.FBX");


	}
}
