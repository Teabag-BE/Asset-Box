package io.teabag.assetbox.post.integration;

import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.repository.FileRepository;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.file.service.S3FileStorageService;
import io.teabag.assetbox.file.service.StoragePurgeService;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostCreateRequest;
import io.teabag.assetbox.post.dto.PostReadResponse;
import io.teabag.assetbox.post.dto.PostResponse;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.post.service.PostService;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.repository.UserRepository;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Asset 게시글 파일 생명주기 통합 테스트")
class AssetPostFileLifecycleIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private FileService fileService;

    @Autowired
    private StoragePurgeService storagePurgeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FileRepository fileRepository;

    @MockitoBean
    private S3FileStorageService s3FileStorageService;

    @MockitoBean
    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() {
        given(s3FileStorageService.uploadWiths3key(any(MultipartFile.class), anyString()))
            .willAnswer(invocation -> invocation.getArgument(1));
        given(s3FileStorageService.createShowPresignedUrl(anyString()))
            .willAnswer(invocation -> "show-url:" + invocation.getArgument(0));
        given(s3FileStorageService.createDownloadPresignedUrl(anyString(), anyString()))
            .willAnswer(invocation -> "download-url:" + invocation.getArgument(0));
    }

    @Test
    @DisplayName("ZIP 업로드부터 상세/다운로드/삭제예약/purge까지 전체 흐름이 정상 동작한다")
    void assetPostFileLifecycle() throws IOException {
        // given
        User user = userRepository.save(createUser());
        CurrentUser currentUser = CurrentUser.from(user);
        MockMultipartFile thumbnail = new MockMultipartFile(
            "thumbnail",
            "thumbnail.png",
            "image/png",
            "thumbnail".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile assetZip = new MockMultipartFile(
            "assetZip",
            "chair_asset.zip",
            "application/zip",
            createAssetZip()
        );
        PostCreateRequest request = new PostCreateRequest(
            "의자 모델",
            "Low poly chair asset",
            1L,
            List.of("chair", "lowpoly"),
            null
        );

        // when: ZIP 업로드
        PostResponse created = postService.save(currentUser, request, thumbnail, assetZip);
        Long postId = created.id();

        // then: DB에는 ZIP, MODEL, TEXTURE 메타데이터가 저장된다
        List<File> uploadedFiles = fileRepository.findByPurposeAndPurposeIdAndDeletedAtIsNullOrderByUploadOrderAsc(
            FilePurpose.ASSET,
            postId
        );
        assertThat(uploadedFiles)
            .extracting(File::getFileType)
            .containsExactly(
                AssetFileType.ZIP,
                AssetFileType.MODEL,
                AssetFileType.TEXTURE,
                AssetFileType.TEXTURE
            );

        File zipFile = uploadedFiles.getFirst();

        // when: 상세 조회
        PostReadResponse detail = postService.getPost(postId);

        // then: 상세 files/downloadFile은 ZIP, viewer는 MODEL/TEXTURE만 가진다
        assertThat(detail.files())
            .singleElement()
            .satisfies(file -> assertThat(file.fileType()).isEqualTo(AssetFileType.ZIP));
        assertThat(detail.downloadFile().fileId()).isEqualTo(zipFile.getId());
        assertThat(detail.downloadFile().fileType()).isEqualTo(AssetFileType.ZIP);
        assertThat(detail.viewer().model().fileType()).isEqualTo(AssetFileType.MODEL);
        assertThat(detail.viewer().model().accessUrl()).startsWith("show-url:");
        assertThat(detail.viewer().textures())
            .hasSize(2)
            .allSatisfy(texture -> {
                assertThat(texture.fileType()).isEqualTo(AssetFileType.TEXTURE);
                assertThat(texture.accessUrl()).startsWith("show-url:");
            });

        // when: 다운로드 URL 요청
        String downloadUrl = fileService.getDownloadPresignedUrl(detail.downloadFile().fileId());

        // then: 원본 ZIP으로 presigned download URL을 생성한다
        assertThat(downloadUrl).isEqualTo("download-url:" + zipFile.getS3Key());
        then(s3FileStorageService).should().createDownloadPresignedUrl(
            eq(zipFile.getS3Key()),
            eq(zipFile.getOriginalName())
        );

        // when: 게시글 삭제
        postService.deletePost(postId);

        // then: 게시글과 파일 메타데이터는 soft delete + purgeAt 설정, S3는 즉시 삭제하지 않는다
        Post deletedPost = postRepository.findById(postId).orElseThrow();
        assertThat(deletedPost.getDeletedAt()).isNotNull();
        assertThat(deletedPost.getThumbnailPurgeAt()).isNotNull();

        assertThat(uploadedFiles)
            .allSatisfy(file -> {
                assertThat(file.getDeletedAt()).isNotNull();
                assertThat(file.getPurgeAt()).isNotNull();
                assertThat(file.getStorageDeletedAt()).isNull();
            });
        then(s3FileStorageService).should(never()).delete(anyString());

        // when: 보관 기간이 지난 뒤 purge 배치 실행
        uploadedFiles.forEach(file -> file.markDeletedWithRetention(Duration.ofDays(-1)));
        deletedPost.markThumbnailDeletedWithRetention(Duration.ofDays(-1));
        fileRepository.flush();
        postRepository.flush();

        storagePurgeService.purgeExpiredStorageObjects();

        // then: S3 객체를 실제 삭제하고 storageDeletedAt을 기록한다
        assertThat(uploadedFiles)
            .allSatisfy(file -> assertThat(file.getStorageDeletedAt()).isNotNull());
        assertThat(deletedPost.getThumbnailStorageDeletedAt()).isNotNull();

        uploadedFiles.forEach(file ->
            then(s3FileStorageService).should().delete(file.getS3Key())
        );
        then(s3FileStorageService).should().delete(deletedPost.getThumbnailKey());
    }

    private User createUser() {
        return User.builder()
            .email("asset-user@naver.com")
            .password("password")
            .name("에셋유저")
            .nickname("assetUser")
            .major(Major.BACK_END)
            .build();
    }

    private byte[] createAssetZip() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            addZipEntry(zipOutputStream, "model/chair.glb", "glb-content");
            addZipEntry(zipOutputStream, "textures/basecolor.png", "png-content");
            addZipEntry(zipOutputStream, "textures/normal.jpg", "jpg-content");
        }

        return outputStream.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }
}
