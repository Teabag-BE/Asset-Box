// package io.teabag.assetbox.file.service.upload;
//
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.*;
//
// import java.util.UUID;
//
// import org.junit.jupiter.api.Test;
// import org.mockito.ArgumentCaptor;
// import org.springframework.mock.web.MockMultipartFile;
//
// import io.teabag.assetbox.file.domain.AssetFileType;
// import io.teabag.assetbox.file.domain.File;
// import io.teabag.assetbox.file.domain.FilePurpose;
// import io.teabag.assetbox.file.repository.FileRepository;
// import io.teabag.assetbox.user.domain.User;
//
// public class FileUploadServiceImplTest {
//
// 	private FileRepository fileRepository;
// 	private S3FileUploadStorageService s3FileUploadStorageService;
// 	private FileUploadServiceImpl fileUploadService;
//
// 	void setUp(){
// 		fileRepository = mock(FileRepository.class);
// 		s3FileUploadStorageService = mock(S3FileUploadStorageService.class);
//
// 		FileUploadValidator fileUploadValidator = new FileUploadValidator();
// 		S3FileKeyGenerator s3FileKeyGenerator = new S3FileKeyGenerator(fileUploadValidator);
//
// 		fileUploadService = new FileUploadServiceImpl(
// 			fileRepository,
// 			s3FileUploadStorageService,
// 			fileUploadValidator,
// 			s3FileKeyGenerator
// 			);
// 	}
//
// 	@Test
// 	void file_파일을_업로드하면_s3에_저장하고_파일_메타데이터를_DB에_저장한다(){
// 		//given
// 		MockMultipartFile multipartFile = new MockMultipartFile(
// 			"file",
// 			"tree.FBX",
// 			"application/octet-stream",
// 			"test".getBytes()
// 		);
//
// 		FilePurpose purpose = FilePurpose.ASSET;
// 		Long purposeId = 1L;
// 		AssetFileType fileType = AssetFileType.MODEL;
// 		UUID uploadBatchId = UUID.fromString("9c54f9e1-0c2a-43cb-a70f-97b9a0b3b123");
// 		Long sortOrder = 1L;
// 		User uploadedBy = mock(User.class);
//
// 		when(fileRepository.save(any(File.class)))
// 			.thenAnswer(invocation -> invocation.getArgument(0));
//
// 		//when
// 		fileUploadService.upload(
// 			multipartFile,
// 			purpose,
// 			purposeId,
// 			fileType,
// 			uploadBatchId,
// 			sortOrder,
// 			uploadedBy
// 		);
//
// 		//then
// 		verify(s3FileUploadStorageService)
// 			.upload(eq(multipartFile), anyString());
//
// 		ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
// 		verify(fileRepository).save(fileCaptor.capture());
//
// 		File savedFile = fileCaptor.getValue();
//
// 		assertThat(savedFile.getOriginalName()).isEqualTo("tree.FBX");
// 		assertThat(savedFile.getSavedName()).startsWith("assets/asset/1/model/");
// 		assertThat(savedFile.getSavedName()).endsWith(".fbx");
// 		assertThat(savedFile.getExtension()).isEqualTo("fbx");
// 		assertThat(savedFile.getSizeBytes()).isEqualTo(multipartFile.getSize());
// 		assertThat(savedFile.getPurpose()).isEqualTo(FilePurpose.ASSET);
// 		assertThat(savedFile.getPurposeId()).isEqualTo(1L);
// 		assertThat(savedFile.getFileType()).isEqualTo(AssetFileType.MODEL);
// 		assertThat(savedFile.getUploadBatchId()).isEqualTo(uploadBatchId.toString());
// 		assertThat(savedFile.getUploadOrder()).isEqualTo(1);
// 		assertThat(savedFile.getUploadedBy()).isEqualTo(uploadedBy);
//
//
// 	}
//
//
//
// }
