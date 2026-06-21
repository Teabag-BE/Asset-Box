package io.teabag.assetbox.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.file.repository.FileRepository;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private S3FileStorageService s3FileStorageService;

    @Mock
    private UserRepository userRepository;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        FileValidator fileValidator = new FileValidator();
        S3FileKeyGenerator s3FileKeyGenerator = new S3FileKeyGenerator(fileValidator);

        fileService = new FileServiceImpl(
            fileRepository,
            s3FileStorageService,
            fileValidator,
            s3FileKeyGenerator,
            userRepository
        );
    }

    @Test
    @DisplayName("파일을 업로드하면 S3에 저장하고 파일 메타데이터를 DB에 저장한다")
    void uploadFiles_savesFileToS3AndDatabase() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "tree.FBX",
            "application/octet-stream",
            "test".getBytes()
        );
        FilePurpose purpose = FilePurpose.ASSET;
        Long purposeId = 1L;
        AssetFileType fileType = AssetFileType.MODEL;
        UUID uploadBatchId = UUID.fromString("9c54f9e1-0c2a-43cb-a70f-97b9a0b3b123");
        User uploadedBy = createUser();

        given(fileRepository.sumSizeBytesByPurposeAndPurposeId(purpose, purposeId))
            .willReturn(0L);
        given(fileRepository.save(any(File.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        FileUploadResponse response = fileService.uploadFiles(
            List.of(file),
            purpose,
            purposeId,
            fileType,
            uploadBatchId,
            uploadedBy
        );

        // then
        then(s3FileStorageService).should().upload(file, response.files().getFirst().savedName());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        then(fileRepository).should().save(fileCaptor.capture());

        File savedFile = fileCaptor.getValue();
        assertThat(savedFile.getOriginalName()).isEqualTo("tree.FBX");
        assertThat(savedFile.getS3Key()).startsWith("assets/asset/1/model/");
        assertThat(savedFile.getS3Key()).endsWith(".fbx");
        assertThat(savedFile.getExtension()).isEqualTo("fbx");
        assertThat(savedFile.getSizeBytes()).isEqualTo(file.getSize());
        assertThat(savedFile.getPurpose()).isEqualTo(purpose);
        assertThat(savedFile.getPurposeId()).isEqualTo(purposeId);
        assertThat(savedFile.getFileType()).isEqualTo(fileType);
        assertThat(savedFile.getUploadBatchId()).isEqualTo(uploadBatchId.toString());
        assertThat(savedFile.getUploadOrder()).isEqualTo(1L);
        assertThat(savedFile.getUploadedBy()).isEqualTo(uploadedBy);
    }

    @Test
    @DisplayName("여러 파일을 업로드하면 업로드 순서를 1부터 저장한다")
    void uploadFiles_savesMultipleFilesWithUploadOrder() {
        // given
        MockMultipartFile model = new MockMultipartFile(
            "files",
            "tree.fbx",
            "application/octet-stream",
            "model".getBytes()
        );
        MockMultipartFile texture = new MockMultipartFile(
            "files",
            "tree.png",
            "image/png",
            "texture".getBytes()
        );
        FilePurpose purpose = FilePurpose.ASSET;
        Long purposeId = 1L;
        UUID uploadBatchId = UUID.fromString("9c54f9e1-0c2a-43cb-a70f-97b9a0b3b123");
        User uploadedBy = createUser();

        given(fileRepository.sumSizeBytesByPurposeAndPurposeId(purpose, purposeId))
            .willReturn(0L);
        given(fileRepository.save(any(File.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        FileUploadResponse response = fileService.uploadFiles(
            List.of(model, texture),
            purpose,
            purposeId,
            List.of(AssetFileType.MODEL, AssetFileType.TEXTURE),
            uploadBatchId,
            uploadedBy
        );

        // then
        assertThat(response.files()).hasSize(2);
        then(s3FileStorageService).should(times(2)).upload(any(MultipartFile.class), anyString());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        then(fileRepository).should(times(2)).save(fileCaptor.capture());

        List<File> savedFiles = fileCaptor.getAllValues();
        assertThat(savedFiles)
            .extracting(File::getUploadOrder)
            .containsExactly(1L, 2L);
        assertThat(savedFiles)
            .extracting(File::getFileType)
            .containsExactly(AssetFileType.MODEL, AssetFileType.TEXTURE);
    }

    @Test
    @DisplayName("검증에 실패한 파일은 S3 업로드와 DB 저장을 하지 않는다")
    void uploadFiles_doesNotSaveWhenValidationFails() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "virus.exe",
            "application/octet-stream",
            "test".getBytes()
        );

        // when
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileService.uploadFiles(
                List.of(file),
                FilePurpose.ASSET,
                1L,
                AssetFileType.MODEL,
                UUID.randomUUID(),
                createUser()
            ))
            .isInstanceOf(BusinessException.class);

        // then
        then(s3FileStorageService).should(never()).upload(any(MultipartFile.class), anyString());
        then(fileRepository).should(never()).save(any(File.class));
    }

    @Test
    @DisplayName("파일 다운로드 presigned URL을 반환한다")
    void getDownloadPresignedUrl_returnsPresignedUrl() {
        // given
        Long fileId = 1L;
        File file = createFile("tree.fbx", "assets/asset/1/model/tree.fbx", 1L);
        given(fileRepository.findById(fileId))
            .willReturn(Optional.of(file));
        given(s3FileStorageService.createDownloadPresignedUrl(file.getS3Key(), file.getOriginalName()))
            .willReturn("https://download-url");

        // when
        String presignedUrl = fileService.getDownloadPresignedUrl(fileId);

        // then
        assertThat(presignedUrl).isEqualTo("https://download-url");
        then(s3FileStorageService).should()
            .createDownloadPresignedUrl(file.getS3Key(), file.getOriginalName());
    }

    @Test
    @DisplayName("존재하지 않는 파일의 다운로드 URL을 요청하면 BusinessException을 던진다")
    void getDownloadPresignedUrl_throwsExceptionWhenFileNotFound() {
        // given
        Long fileId = 1L;
        given(fileRepository.findById(fileId))
            .willReturn(Optional.empty());

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileService.getDownloadPresignedUrl(fileId))
            .isInstanceOf(BusinessException.class);

        then(s3FileStorageService).should(never())
            .createDownloadPresignedUrl(anyString(), anyString());
    }

    @Test
    @DisplayName("purpose와 purposeId로 미리보기 presigned URL 목록을 반환한다")
    void getShowPresignedUrlsByPurpose_returnsPresignedUrls() {
        // given
        File firstFile = createFile("tree.fbx", "assets/asset/1/model/tree.fbx", 1L);
        File secondFile = createFile("tree.png", "assets/asset/1/texture/tree.png", 2L);

        given(fileRepository.findByPurposeAndPurposeId(FilePurpose.ASSET, 1L))
            .willReturn(List.of(firstFile, secondFile));
        given(s3FileStorageService.createShowPresignedUrl(firstFile.getS3Key()))
            .willReturn("https://show-url-1");
        given(s3FileStorageService.createShowPresignedUrl(secondFile.getS3Key()))
            .willReturn("https://show-url-2");

        // when
        List<String> presignedUrls = fileService.getShowPresignedUrlsByPurpose("ASSET", 1L);

        // then
        assertThat(presignedUrls).containsExactly("https://show-url-1", "https://show-url-2");
    }

    @Test
    @DisplayName("파일 첨부 목록을 업로드 순서대로 반환한다")
    void getFileAttachmentsByPurpose_returnsAttachmentsWithAccessUrl() {
        // given
        File firstFile = createFile("tree.fbx", "assets/asset/1/model/tree.fbx", 1L);
        File secondFile = createFile("tree.png", "assets/asset/1/texture/tree.png", 2L);

        given(fileRepository.findByPurposeAndPurposeIdOrderByUploadOrderAsc(FilePurpose.ASSET, 1L))
            .willReturn(List.of(firstFile, secondFile));
        given(s3FileStorageService.createShowPresignedUrl(firstFile.getS3Key()))
            .willReturn("https://show-url-1");
        given(s3FileStorageService.createShowPresignedUrl(secondFile.getS3Key()))
            .willReturn("https://show-url-2");

        // when
        List<FileAttachmentResponse> attachments = fileService.getFileAttachmentsByPurpose(FilePurpose.ASSET, 1L);

        // then
        assertThat(attachments).hasSize(2);
        assertThat(attachments)
            .extracting(FileAttachmentResponse::accessUrl)
            .containsExactly("https://show-url-1", "https://show-url-2");
        assertThat(attachments)
            .extracting(FileAttachmentResponse::uploadOrder)
            .containsExactly(1L, 2L);
    }

    private User createUser() {
        return User.builder()
            .email("test@naver.com")
            .password("password")
            .name("테스트")
            .nickname("tester")
            .major(Major.BACK_END)
            .build();
    }

    private File createFile(String originalName, String s3Key, Long uploadOrder) {
        String extension = originalName.substring(originalName.lastIndexOf(".") + 1);

        return File.builder()
            .originalName(originalName)
            .s3Key(s3Key)
            .extension(extension)
            .sizeBytes(100L)
            .purpose(FilePurpose.ASSET)
            .purposeId(1L)
            .uploadedBy(createUser())
            .uploadOrder(uploadOrder)
            .fileType(uploadOrder == 1L ? AssetFileType.MODEL : AssetFileType.TEXTURE)
            .uploadBatchId("9c54f9e1-0c2a-43cb-a70f-97b9a0b3b123")
            .build();
    }
}
