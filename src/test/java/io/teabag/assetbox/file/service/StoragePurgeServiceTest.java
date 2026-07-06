package io.teabag.assetbox.file.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.File;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.repository.FileRepository;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class StoragePurgeServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private S3FileStorageService s3FileStorageService;

    private StoragePurgeService storagePurgeService;

    @BeforeEach
    void setUp() {
        storagePurgeService = new StoragePurgeService(
            fileRepository,
            postRepository,
            s3FileStorageService
        );
    }

    @Test
    @DisplayName("purgeAt이 지난 파일과 썸네일을 S3에서 삭제하고 storageDeletedAt을 기록한다")
    void purgeExpiredStorageObjects_deletesExpiredObjectsAndMarksStorageDeleted() {
        // given
        File file = createFile("asset.zip", "posts/1/original/batch.zip", AssetFileType.ZIP);
        file.markDeletedWithRetention(Duration.ofDays(-1));
        Post post = createPostWithThumbnail("thumbnail-key");
        post.markThumbnailDeletedWithRetention(Duration.ofDays(-1));

        given(fileRepository.findByDeletedAtIsNotNullAndPurgeAtLessThanEqualAndStorageDeletedAtIsNull(any(LocalDateTime.class)))
            .willReturn(List.of(file));
        given(postRepository.findByThumbnailKeyIsNotNullAndThumbnailPurgeAtLessThanEqualAndThumbnailStorageDeletedAtIsNull(any(LocalDateTime.class)))
            .willReturn(List.of(post));

        // when
        storagePurgeService.purgeExpiredStorageObjects();

        // then
        then(s3FileStorageService).should().delete("posts/1/original/batch.zip");
        then(s3FileStorageService).should().delete("thumbnail-key");
        assertThat(file.getStorageDeletedAt()).isNotNull();
        assertThat(post.getThumbnailStorageDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("S3 삭제에 실패하면 storageDeletedAt을 기록하지 않고 다음 배치에서 재시도할 수 있게 둔다")
    void purgeExpiredStorageObjects_doesNotMarkStorageDeletedWhenDeleteFails() {
        // given
        File file = createFile("asset.zip", "posts/1/original/batch.zip", AssetFileType.ZIP);
        file.markDeletedWithRetention(Duration.ofDays(-1));

        given(fileRepository.findByDeletedAtIsNotNullAndPurgeAtLessThanEqualAndStorageDeletedAtIsNull(any(LocalDateTime.class)))
            .willReturn(List.of(file));
        given(postRepository.findByThumbnailKeyIsNotNullAndThumbnailPurgeAtLessThanEqualAndThumbnailStorageDeletedAtIsNull(any(LocalDateTime.class)))
            .willReturn(List.of());
        willThrow(new BusinessException(ErrorCode.STORAGE_DELETE_FAILED))
            .given(s3FileStorageService)
            .delete(file.getS3Key());

        // when
        storagePurgeService.purgeExpiredStorageObjects();

        // then
        assertThat(file.getStorageDeletedAt()).isNull();
    }

    private File createFile(String originalName, String s3Key, AssetFileType fileType) {
        return File.builder()
            .originalName(originalName)
            .s3Key(s3Key)
            .extension(originalName.substring(originalName.lastIndexOf(".") + 1))
            .sizeBytes(100L)
            .purpose(FilePurpose.ASSET)
            .purposeId(1L)
            .uploadedBy(createUser())
            .uploadOrder(1L)
            .fileType(fileType)
            .uploadBatchId("9c54f9e1-0c2a-43cb-a70f-97b9a0b3b123")
            .build();
    }

    private Post createPostWithThumbnail(String thumbnailKey) {
        Post post = Post.builder()
            .title("제목")
            .content("내용")
            .authorId(1L)
            .categoryId(1L)
            .linkedRequestId(null)
            .build();
        post.setThumbnailKey(thumbnailKey);
        return post;
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
}
