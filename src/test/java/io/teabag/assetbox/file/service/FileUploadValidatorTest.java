package io.teabag.assetbox.file.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("FileValidator의")
class FileUploadValidatorTest {

	private final FileValidator validator = new FileValidator();

	@Nested
	@DisplayName("Describe: validate() 메서드는")
	class Describe_validate {

		@Nested
		@DisplayName("Context: 허용된 파일이 주어지면")
		class Context_with_valid_file {

			@Test
			@DisplayName("It: 검증을 통과한다")
			void it_passes_validation() {
				// given
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
		}

		@Nested
		@DisplayName("Context: null 파일이 주어지면")
		class Context_with_null_file {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// when & then
				assertThatThrownBy(() -> validator.validate(null))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.FILE_EMPTY.getDescription());
			}
		}

		@Nested
		@DisplayName("Context: 허용되지 않은 확장자의 파일이 주어지면")
		class Context_with_invalid_extension {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// given
				MockMultipartFile file = new MockMultipartFile(
					"file",
					"virus.exe",
					"application/octet-stream",
					"test".getBytes()
				);

				// when & then
				assertThatThrownBy(() -> validator.validate(file))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.EXTENSIONS_INVALID.getDescription());
			}
		}

		@Nested
		@DisplayName("Context: 빈 파일이 주어지면")
		class Context_with_empty_file {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// given
				MockMultipartFile file = new MockMultipartFile(
					"file",
					"empty.fbx",
					"application/octet-stream",
					new byte[0]
				);

				// when & then
				assertThatThrownBy(() -> validator.validate(file))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.FILE_EMPTY.getDescription());
			}
		}

		@Nested
		@DisplayName("Context: 20MB를 초과하는 파일이 주어지면")
		class Context_with_over_20mb_file {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// given
				byte[] over20MB = new byte[20 * 1024 * 1024 + 1];
				MockMultipartFile file = new MockMultipartFile(
					"file",
					"big.fbx",
					"application/octet-stream",
					over20MB
				);

				// when & then
				assertThatThrownBy(() -> validator.validate(file))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.SIZE_INVALID.getDescription());
			}
		}
	}

	@Nested
	@DisplayName("Describe: validateFilesTotalSize() 메서드는")
	class Describe_validateFilesTotalSize {

		@Nested
		@DisplayName("Context: 기존 파일과 새 파일의 합이 20MB 이하이면")
		class Context_with_total_size_under_limit {

			@Test
			@DisplayName("It: 검증을 통과한다")
			void it_passes_validation() {
				// given
				long currentTotalSizeBytes = 20 * 1024 * 1024L - 1;
				MockMultipartFile file = new MockMultipartFile(
					"file",
					"texture.png",
					"image/png",
					new byte[] { 1 }
				);

				// when & then
				assertThatCode(() -> validator.validateFilesTotalSize(currentTotalSizeBytes, List.of(file)))
					.doesNotThrowAnyException();
			}
		}

		@Nested
		@DisplayName("Context: 기존 파일과 새 파일의 합이 20MB를 초과하면")
		class Context_with_total_size_over_limit {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// given
				long currentTotalSizeBytes = 20 * 1024 * 1024L;
				MockMultipartFile file = new MockMultipartFile(
					"file",
					"texture.png",
					"image/png",
					new byte[] { 1 }
				);

				// when & then
				assertThatThrownBy(() -> validator.validateFilesTotalSize(currentTotalSizeBytes, List.of(file)))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.FILE_TOTAL_SIZE_INVALID.getDescription());
			}
		}
	}

	@Nested
	@DisplayName("Describe: validateThumbnail() 메서드는")
	class Describe_validateThumbnail {

		@Nested
		@DisplayName("Context: 허용된 이미지 파일이 주어지면")
		class Context_with_valid_image {

			@Test
			@DisplayName("It: 검증을 통과한다")
			void it_passes_validation() {
				// given
				MockMultipartFile file = new MockMultipartFile(
					"file",
					"thumbnail.PNG",
					"image/png",
					"test".getBytes()
				);

				// when & then
				assertThatCode(() -> validator.validateThumbnail(file))
					.doesNotThrowAnyException();
			}
		}

		@Nested
		@DisplayName("Context: 이미지가 아닌 파일이 주어지면")
		class Context_with_non_image_file {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// given
				MockMultipartFile file = new MockMultipartFile(
					"file",
					"model.fbx",
					"application/octet-stream",
					"test".getBytes()
				);

				// when & then
				assertThatThrownBy(() -> validator.validateThumbnail(file))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.EXTENSIONS_INVALID.getDescription());
			}
		}

		@Nested
		@DisplayName("Context: 5MB를 초과하는 썸네일이 주어지면")
		class Context_with_over_5mb_thumbnail {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// given
				byte[] over5MB = new byte[5 * 1024 * 1024 + 1];
				MockMultipartFile file = new MockMultipartFile(
					"file",
					"thumbnail.png",
					"image/png",
					over5MB
				);

				// when & then
				assertThatThrownBy(() -> validator.validateThumbnail(file))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.THUMBNAIL_SIZE_INVALID.getDescription());
			}
		}

		@Nested
		@DisplayName("Context: 썸네일 파일이 주어지지 않으면(null이면)")
		class Context_with_null_file {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// when & then
				assertThatThrownBy(() -> validator.validateThumbnail(null))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.FILE_EMPTY.getDescription());
			}
		}

		@Nested
		@DisplayName("Context: 빈 썸네일 파일이 주어지면")
		class Context_with_empty_file {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// given
				MockMultipartFile file = new MockMultipartFile(
					"file",
					"thumbnail.png",
					"image/png",
					new byte[0]
				);

				// when & then
				assertThatThrownBy(() -> validator.validateThumbnail(file))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.FILE_EMPTY.getDescription());
			}
		}
	}

	@Nested
	@DisplayName("Describe: extractExtension() 메서드는")
	class Describe_extractExtension {

		@Nested
		@DisplayName("Context: 대문자 확장자가 포함된 파일명이 주어지면")
		class Context_with_uppercase_extension {

			@Test
			@DisplayName("It: 확장자를 소문자로 반환한다")
			void it_returns_lowercase_extension() {
				// when
				String extension = validator.extractExtension("tree.FBX");

				// then
				assertThat(extension).isEqualTo("fbx");
			}
		}

		@Nested
		@DisplayName("Context: 확장자가 없는 파일명이 주어지면")
		class Context_without_extension {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// when & then
				assertThatThrownBy(() -> validator.extractExtension("tree"))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.EXTENSIONS_INVALID.getDescription());
			}
		}

		@Nested
		@DisplayName("Context: 파일명이 null이면")
		class Context_with_null_filename {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// when & then
				assertThatThrownBy(() -> validator.extractExtension(null))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.FILE_NAME_EMPTY.getDescription());
			}
		}

		@Nested
		@DisplayName("Context: 파일명이 빈 문자열이면")
		class Context_with_blank_filename {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// when & then
				assertThatThrownBy(() -> validator.extractExtension(" "))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.FILE_NAME_EMPTY.getDescription());
			}
		}

		@Nested
		@DisplayName("Context: 파일명이 점으로 끝나면")
		class Context_with_filename_ending_dot {

			@Test
			@DisplayName("It: BusinessException을 던진다")
			void it_throws_business_exception() {
				// when & then
				assertThatThrownBy(() -> validator.extractExtension("tree."))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining(ErrorCode.EXTENSIONS_INVALID.getDescription());
			}
		}
	}

}
