package io.teabag.assetbox.file.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import io.teabag.assetbox.common.exception.BusinessException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class S3FileStorageServiceTest {

	// 진짜 S3에 요청하지 않음
	@Mock
	private S3Client s3Client;

	@Mock
	private S3Presigner s3Presigner;

	private S3FileStorageService s3FileStorageService;

	@BeforeEach
	void setup(){
		s3FileStorageService = new S3FileStorageService(s3Client, s3Presigner);

		// bucket 값 강제로 넣기
		// yml 파일의 bucket이 아닌 test-bucket으로
		ReflectionTestUtils.setField(
			s3FileStorageService,
			"bucket",
			"test-bucket"
		);
	}

	@Test
	@DisplayName("S3에 파일을 업로드한다")
	void uploadFileToS3() {
		// given
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"tree.png",
			"image/png",
			"test".getBytes()
		);
		String s3Key = "assets/asset/1/texture/tree.png";
		ArgumentCaptor<PutObjectRequest> captor =
			ArgumentCaptor.forClass(PutObjectRequest.class);

		// when
		s3FileStorageService.upload(file, s3Key);

		// then
		then(s3Client).should().putObject(captor.capture(), any(RequestBody.class));

		PutObjectRequest request = captor.getValue();
		assertThat(request.bucket()).isEqualTo("test-bucket");
		assertThat(request.key()).isEqualTo(s3Key);
		assertThat(request.contentType()).isEqualTo("image/png");
	}

	@Test
	@DisplayName("S3 파일 업로드에 실패하면 STORAGE_WRITE_FAILED 예외를 던진다")
	void throwExceptionWhenUploadFails() {
		// given
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"tree.png",
			"image/png",
			"test".getBytes()
		);
		String s3Key = "assets/asset/1/texture/tree.png";

		given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
			.willThrow(new RuntimeException("S3 upload failed"));

		// when & then
		assertThatThrownBy(() -> s3FileStorageService.upload(file, s3Key))
			.isInstanceOf(BusinessException.class);
	}

	/*
	s3FileStorageService.delete(s3Key);
	을 통해 s3Client.deleteObject(deleteReq)가 실행됨.
	그래서 then에서 검증함.
	s3Client.deleteObject()가 실제로 호출됐는가?
	그리고 그때 전달된 DeleteObjectRequest를 잡아오겠다.
	 */
	@Test
	@DisplayName("S3 파일을 단건 삭제한다.")
	void deleteFileFromS3(){
		// given
		String s3Key = "assets/2026/06/12/test.png";

		// deleteObjectRequest는 테스트 코드에서 직접 볼 수 없으니, Captor로 요청 객체를 가로침
		ArgumentCaptor<DeleteObjectRequest> captor =
			ArgumentCaptor.forClass(DeleteObjectRequest.class);

		// when
		s3FileStorageService.delete(s3Key);

		//then
		then(s3Client).should().deleteObject(captor.capture());

		DeleteObjectRequest request = captor.getValue();

		assertThat(request.bucket()).isEqualTo("test-bucket");
		assertThat(request.key()).isEqualTo(s3Key);
	}

	@Test
	@DisplayName("S3 파일 삭제에 실패하면 STORAGE_DELETE_FAILED 예외를 던진다")
	void throwExceptionWhenDeleteFails() {
		// given
		String s3Key = "assets/2026/06/12/test.png";

		given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
			.willThrow(new RuntimeException("S3 delete failed"));

		// when & then
		assertThatThrownBy(() -> s3FileStorageService.delete(s3Key))
			.isInstanceOf(BusinessException.class);
	}
}
