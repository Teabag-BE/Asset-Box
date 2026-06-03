package io.teabag.assetbox.file.service.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

public class FileUploadValidatorTest {

	private final FileUploadValidator validator = new FileUploadValidator();

	@Test
	void 허용된_확장자면_검증을_통과한다(){
		//given
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"tree.fbx",
			"application/octet-stream",
			"test".getBytes()
		);

		// when & then
		assertThatCode(() -> validator.validate(file))
			.doesNotThrowAnyException();
	}

	@Test
	void 허용되지_않은_확장자면_예외가_발생한다(){
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"virus.exe",
			"application/actet-stream",
			"test".getBytes()
		);

		assertThatThrownBy(() -> validator.validate(file))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("허용되지 않은 파일 형식");
	}

	@Test
	void 빈_파일이면_예외가_발생한다(){
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"empty.fbx",
			"application/octet-stream",
			new byte[0]
		);

		assertThatThrownBy(() -> validator.validate(file))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("파일이 비어 있습니다.");
	}

	@Test
	void 파일_크기가_20MB를_초과하면_예외가_발생한다() {
		byte[] over20MB = new byte[20 * 1024 * 1024 + 1];

		MockMultipartFile file = new MockMultipartFile(
			"file",
			"big.fbx",
			"application/octet-stream",
			over20MB
		);

		assertThatThrownBy(() -> validator.validate(file))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("파일 크기");

	}

	@Test
	void 파일명에서_확장자를_소문자로_추출한다(){
		String extenstion = validator.extractExtension("tree.FBX");

		assertThat(extenstion).isEqualTo("fbx");

	}

	@Test
	void 확장자가_없는_파일명은_예외발생(){
		assertThatThrownBy(() -> validator.extractExtension("tree"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("파일 확장자가 없습니다");;
	}


}
