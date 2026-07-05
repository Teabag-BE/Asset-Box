package io.teabag.assetbox.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.teabag.assetbox.common.constants.ErrorCode;
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
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private S3FileStorageService s3FileStorageService;

    @Mock
    private UserRepository userRepository;

    private FileServiceImpl fileService;
    private ZipExtractService zipExtractService;

    @BeforeEach
    void setUp() {
        FileValidator fileValidator = new FileValidator();
        S3FileKeyGenerator s3FileKeyGenerator = new S3FileKeyGenerator(fileValidator);
        zipExtractService = spy(new ZipExtractService());

        fileService = new FileServiceImpl(
            fileRepository,
            s3FileStorageService,
            fileValidator,
            s3FileKeyGenerator,
            zipExtractService,
            userRepository
        );
    }

    @Test
    @DisplayName("파일을 업로드하면 S3에 저장하고 파일 메타데이터를 DB에 저장한다")
    void uploadFiles_savesFileToS3AndDatabase() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "reference.PNG",
            "image/png",
            "test".getBytes()
        );
        FilePurpose purpose = FilePurpose.REQUEST_REFERENCE;
        Long purposeId = 1L;
        AssetFileType fileType = AssetFileType.REFERENCE;
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
            uploadBatchId,
            uploadedBy
        );

        // then
        then(s3FileStorageService).should().upload(file, response.files().getFirst().savedName());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        then(fileRepository).should().save(fileCaptor.capture());

        File savedFile = fileCaptor.getValue();
        assertThat(savedFile.getOriginalName()).isEqualTo("reference.PNG");
        assertThat(savedFile.getS3Key()).startsWith("assets/request_reference/1/reference/");
        assertThat(savedFile.getS3Key()).endsWith(".png");
        assertThat(savedFile.getExtension()).isEqualTo("png");
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
            "reference.png",
            "image/png",
            "reference".getBytes()
        );
        MockMultipartFile texture = new MockMultipartFile(
            "files",
            "reference.jpg",
            "image/jpeg",
            "reference2".getBytes()
        );
        FilePurpose purpose = FilePurpose.REQUEST_REFERENCE;
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
            .containsExactly(AssetFileType.REFERENCE, AssetFileType.REFERENCE);
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
        File zipFile = createFile("asset.zip", "posts/1/original/batch.zip", 1L, AssetFileType.ZIP);
        given(fileRepository.findById(fileId))
            .willReturn(Optional.of(file));
        given(fileRepository.findByPurposeAndPurposeIdAndUploadBatchIdAndFileTypeAndDeletedAtIsNull(
            FilePurpose.ASSET,
            1L,
            file.getUploadBatchId(),
            AssetFileType.ZIP
        )).willReturn(Optional.of(zipFile));
        given(s3FileStorageService.createDownloadPresignedUrl(zipFile.getS3Key(), zipFile.getOriginalName()))
            .willReturn("https://download-url");

        // when
        String presignedUrl = fileService.getDownloadPresignedUrl(fileId);

        // then
        assertThat(presignedUrl).isEqualTo("https://download-url");
        then(s3FileStorageService).should()
            .createDownloadPresignedUrl(zipFile.getS3Key(), zipFile.getOriginalName());
    }

    @Test
    @DisplayName("ASSET 파일이 ZIP이 아니면 예외가 발생한다")
    void uploadAssetZip_throwsExceptionWhenFileIsNotZip() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "tree.fbx",
            "application/octet-stream",
            "model".getBytes()
        );

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileService.uploadFiles(
                List.of(file),
                FilePurpose.ASSET,
                1L,
                UUID.randomUUID(),
                createUser()
            ))
            .isInstanceOf(BusinessException.class);

        then(s3FileStorageService).should(never()).upload(any(Path.class), anyString(), anyString());
        then(fileRepository).should(never()).save(any(File.class));
    }

    @Test
    @DisplayName("정상 ZIP 업로드 시 ZIP, MODEL, TEXTURE 메타데이터를 저장하고 S3에 업로드한다")
    void uploadAssetZip_savesZipModelAndTexture() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "asset.zip",
            "application/zip",
            createZipBytes(
                new ZipTestEntry("model.fbx", "model".getBytes()),
                new ZipTestEntry("textures/basecolor.png", "texture".getBytes())
            )
        );
        UUID uploadBatchId = UUID.fromString("9c54f9e1-0c2a-43cb-a70f-97b9a0b3b123");
        User uploadedBy = createUser();

        given(fileRepository.sumSizeBytesByPurposeAndPurposeId(FilePurpose.ASSET, 10L))
            .willReturn(0L);
        given(fileRepository.save(any(File.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        FileUploadResponse response = fileService.uploadFiles(
            List.of(file),
            FilePurpose.ASSET,
            10L,
            uploadBatchId,
            uploadedBy
        );

        // then
        assertThat(response.files()).hasSize(3);
        assertThat(response.files())
            .extracting(fileResponse -> fileResponse.fileType())
            .containsExactly(AssetFileType.ZIP, AssetFileType.MODEL, AssetFileType.TEXTURE);

        then(s3FileStorageService).should(times(3)).upload(any(Path.class), anyString(), anyString());

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        then(fileRepository).should(times(3)).save(fileCaptor.capture());

        List<File> savedFiles = fileCaptor.getAllValues();
        assertThat(savedFiles)
            .extracting(File::getS3Key)
            .anySatisfy(s3Key -> assertThat(s3Key).isEqualTo("posts/10/original/" + uploadBatchId + ".zip"))
            .anySatisfy(s3Key -> assertThat(s3Key).startsWith("posts/10/viewer/model/").endsWith(".fbx"))
            .anySatisfy(s3Key -> assertThat(s3Key).startsWith("posts/10/viewer/textures/").endsWith("_basecolor.png"));
        assertThat(savedFiles)
            .extracting(File::getUploadBatchId)
            .containsOnly(uploadBatchId.toString());

        ArgumentCaptor<Path> extractDirCaptor = ArgumentCaptor.forClass(Path.class);
        then(zipExtractService).should().extractAssetZip(any(Path.class), extractDirCaptor.capture());
        assertThat(Files.exists(extractDirCaptor.getValue().getParent())).isFalse();
    }

    @Test
    @DisplayName("ASSET ZIP 업로드 중 실패하면 이미 업로드된 S3 파일을 삭제한다")
    void uploadAssetZip_deletesUploadedS3KeysWhenUploadFails() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "asset.zip",
            "application/zip",
            createZipBytes(
                new ZipTestEntry("model.fbx", "model".getBytes()),
                new ZipTestEntry("basecolor.png", "texture".getBytes())
            )
        );

        given(fileRepository.sumSizeBytesByPurposeAndPurposeId(FilePurpose.ASSET, 10L))
            .willReturn(0L);
        given(fileRepository.save(any(File.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
                String s3Key = invocation.getArgument(1);
                if (s3Key.contains("/textures/")) {
                    throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED);
                }

                return null;
            })
            .when(s3FileStorageService)
            .upload(any(Path.class), anyString(), anyString());

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileService.uploadFiles(
                List.of(file),
                FilePurpose.ASSET,
                10L,
                UUID.fromString("9c54f9e1-0c2a-43cb-a70f-97b9a0b3b123"),
                createUser()
            ))
            .isInstanceOf(BusinessException.class);

        ArgumentCaptor<String> deleteCaptor = ArgumentCaptor.forClass(String.class);
        then(s3FileStorageService).should(times(2)).delete(deleteCaptor.capture());
        assertThat(deleteCaptor.getAllValues())
            .anySatisfy(s3Key -> assertThat(s3Key).startsWith("posts/10/original/"))
            .anySatisfy(s3Key -> assertThat(s3Key).startsWith("posts/10/viewer/model/"));
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

        given(fileRepository.findByPurposeAndPurposeIdAndDeletedAtIsNullOrderByUploadOrderAsc(FilePurpose.ASSET, 1L))
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

    @Test
    @DisplayName("purpose와 purposeId로 연결된 파일들의 S3 객체를 삭제하고 메타데이터를 soft delete 한다")
    void deleteFilesByPurpose_deletesStorageObjectsAndSoftDeletesMetadata() {
        // given
        File zip = createFile("asset.zip", "posts/1/original/batch.zip", 1L, AssetFileType.ZIP);
        File model = createFile("model.fbx", "posts/1/viewer/model/model.fbx", 2L, AssetFileType.MODEL);
        File texture = createFile("basecolor.png", "posts/1/viewer/textures/basecolor.png", 3L, AssetFileType.TEXTURE);

        given(fileRepository.findByPurposeAndPurposeIdAndDeletedAtIsNullOrderByUploadOrderAsc(FilePurpose.ASSET, 1L))
            .willReturn(List.of(zip, model, texture));

        // when
        fileService.deleteFilesByPurpose(FilePurpose.ASSET, 1L);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> s3KeysCaptor = ArgumentCaptor.forClass(List.class);
        then(s3FileStorageService).should().deleteAll(s3KeysCaptor.capture());
        assertThat(s3KeysCaptor.getValue())
            .containsExactly(
                "posts/1/original/batch.zip",
                "posts/1/viewer/model/model.fbx",
                "posts/1/viewer/textures/basecolor.png"
            );
        assertThat(zip.getDeletedAt()).isNotNull();
        assertThat(model.getDeletedAt()).isNotNull();
        assertThat(texture.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("연결 파일 S3 삭제에 실패하면 예외가 발생하고 메타데이터를 soft delete 하지 않는다")
    void deleteFilesByPurpose_throwsExceptionWhenStorageDeleteFails() {
        // given
        File zip = createFile("asset.zip", "posts/1/original/batch.zip", 1L, AssetFileType.ZIP);
        given(fileRepository.findByPurposeAndPurposeIdAndDeletedAtIsNullOrderByUploadOrderAsc(FilePurpose.ASSET, 1L))
            .willReturn(List.of(zip));
        doThrow(new BusinessException(ErrorCode.STORAGE_DELETE_FAILED))
            .when(s3FileStorageService)
            .deleteAll(any());

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileService.deleteFilesByPurpose(FilePurpose.ASSET, 1L))
            .isInstanceOf(BusinessException.class);
        assertThat(zip.getDeletedAt()).isNull();
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
        return createFile(originalName, s3Key, uploadOrder, uploadOrder == 1L ? AssetFileType.MODEL : AssetFileType.TEXTURE);
    }

    private File createFile(String originalName, String s3Key, Long uploadOrder, AssetFileType fileType) {
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
            .fileType(fileType)
            .uploadBatchId("9c54f9e1-0c2a-43cb-a70f-97b9a0b3b123")
            .build();
    }

    private byte[] createZipBytes(ZipTestEntry... entries) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (ZipTestEntry entry : entries) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.name()));
                zipOutputStream.write(entry.content());
                zipOutputStream.closeEntry();
            }
        }

        return outputStream.toByteArray();
    }

    private record ZipTestEntry(String name, byte[] content) {
    }
}
