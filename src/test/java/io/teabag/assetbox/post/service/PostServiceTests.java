package io.teabag.assetbox.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.anyList;
import static org.mockito.BDDMockito.nullable;

import java.util.LinkedHashSet;
import java.util.List;

import io.teabag.assetbox.post.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.AssetFileType;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.repository.PostRepository;
import io.teabag.assetbox.tag.domain.Tag;
import io.teabag.assetbox.tag.service.TagService;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.service.UserService;
import io.teabag.assetbox.util.TestUtil;
import io.teabag.assetbox.util.UserUtil;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Transactional
class PostServiceTests {

    @Mock
    TagService tagService;

    @Mock
    PostRepository postRepository;

    @Mock
    FileService fileService;

    @InjectMocks
    PostService postService;

    @Mock
    UserService userService;

    @Nested
    @DisplayName("게시글 생성 관련")
    class postCreate{
        @Test
        @DisplayName("생성 시 태그를 조회하거나 생성한 뒤 게시글을 저장한다")
        void savePost() {
            // given
            MultipartFile thumbnail = new MockMultipartFile(
                    "thumbnail",
                    "thumb.png",
                    "image/png",
                    "test image content".getBytes()
            );

            User user = UserUtil.createUser("user@test.com","password","정수리리");
            ReflectionTestUtils.setField(user, "id", 1L);

            PostCreateRequest request = TestUtil.postCreateRequestOf();
            Tag springTag = new Tag("spring");
            Tag jpaTag = new Tag("jpa");

            CurrentUser currentUser = CurrentUser.from(user);
            MultipartFile assetZip = new MockMultipartFile(
                    "assetZip",
                    "asset.zip",
                    "application/zip",
                    "zip".getBytes()
            );

            given(tagService.findOrCreateAll(request.tags()))
                    .willReturn(new LinkedHashSet<>(List.of(springTag, jpaTag)));
            given(postRepository.save(any(Post.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(userService.currentUserToUser(currentUser))
                    .willReturn(user);

            given(fileService.uploadThumbnail(any(), any(), nullable(Long.class)))
                    .willReturn("thumbnail-key");

            given(fileService.getShowPresignedUrl("thumbnail-key"))
                    .willReturn("thumbnail-url");

            given(fileService.uploadFiles(anyList(), any(), nullable(Long.class), any(), any()))
                    .willReturn(new FileUploadResponse(List.of()));

            // when
            PostResponse savedPost = postService.save(currentUser, request, thumbnail, assetZip);

            // then
            assertThat(savedPost.title()).isEqualTo("제목");
            assertThat(savedPost.content()).isEqualTo("내용");
            assertThat(savedPost.tags()).hasSize(2);
            assertThat(savedPost.authorId()).isEqualTo(1L);

            then(tagService).should().findOrCreateAll(request.tags());
            then(postRepository).should().save(any(Post.class));
        }

        @Test
        @DisplayName("에셋 ZIP 업로드가 실패하면 이미 업로드한 썸네일을 삭제한다")
        void savePost_deletesThumbnailWhenAssetZipUploadFails() {
            // given
            MultipartFile thumbnail = new MockMultipartFile(
                    "thumbnail",
                    "thumb.png",
                    "image/png",
                    "test image content".getBytes()
            );
            MultipartFile assetZip = new MockMultipartFile(
                    "assetZip",
                    "asset.zip",
                    "application/zip",
                    "zip".getBytes()
            );
            User user = UserUtil.createUser("user@test.com","password","정수리리");
            ReflectionTestUtils.setField(user, "id", 1L);
            CurrentUser currentUser = CurrentUser.from(user);
            PostCreateRequest request = TestUtil.postCreateRequestOf();

            given(userService.currentUserToUser(currentUser))
                    .willReturn(user);
            given(postRepository.save(any(Post.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(tagService.findOrCreateAll(request.tags()))
                    .willReturn(new LinkedHashSet<>());
            given(fileService.uploadThumbnail(any(), any(), nullable(Long.class)))
                    .willReturn("thumbnail-key");
            given(fileService.getShowPresignedUrl("thumbnail-key"))
                    .willReturn("thumbnail-url");
            given(fileService.uploadFiles(anyList(), eq(FilePurpose.ASSET), nullable(Long.class), any(), eq(user)))
                    .willThrow(new BusinessException(ErrorCode.STORAGE_WRITE_FAILED));

            // when & then
            assertThatThrownBy(() -> postService.save(currentUser, request, thumbnail, assetZip))
                    .isInstanceOf(BusinessException.class);

            then(fileService).should().deleteStorageObject("thumbnail-key");
        }

    }

    @Nested
    @DisplayName("게시글 삭제 관련")
    class postDelete{
        @Test
        @DisplayName("삭제 시 실제 삭제하지 않고 deletedAt을 채운다")
        void deletePost_success() {
            // given
            Long postId = 1L;

            Post post = createPost(postId);

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);

            // when
            postService.deletePost(postId);

            // then
            assertThat(post.getDeletedAt()).isNotNull();

            then(postRepository)
                    .should()
                    .findByIdOrThrow(postId);

            then(postRepository)
                    .should(never())
                    .delete(any(Post.class));
            then(fileService)
                    .should()
                    .deleteFilesByPurpose(FilePurpose.ASSET, postId);
            then(fileService)
                    .should(never())
                    .deleteStorageObject(anyString());
            assertThat(post.getThumbnailPurgeAt()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 게시글 삭제 시 예외가 발생")
        void deletePost_fail_when_post_not_found() {
            // given
            Long postId = 999L;

            given(postRepository.findByIdOrThrow(postId))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            // when
            assertThatThrownBy(() -> postService.deletePost(postId))
                    .isInstanceOf(BusinessException.class);


            //then
            then(postRepository)
                    .should()
                    .findByIdOrThrow(postId);
            then(postRepository)
                    .should(never())
                    .delete(any(Post.class));
            then(fileService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("게시글 삭제 시 썸네일 삭제 예약 시각을 설정하고 S3 삭제는 시도하지 않는다")
        void deletePost_marksThumbnailForPurgeWhenThumbnailKeyExists() {
            // given
            Long postId = 1L;
            Post post = createPost(postId);
            post.setThumbnailKey("thumbnail-key");

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);

            // when
            postService.deletePost(postId);

            // then
            then(fileService).should(never()).deleteStorageObject(anyString());
            then(fileService).should().deleteFilesByPurpose(FilePurpose.ASSET, postId);
            assertThat(post.getThumbnailPurgeAt()).isNotNull();
            assertThat(post.getThumbnailStorageDeletedAt()).isNull();
        }

        @Test
        @DisplayName("thumbnailKey가 null이면 썸네일 삭제 예약과 S3 삭제를 시도하지 않는다")
        void deletePost_doesNotMarkThumbnailForPurgeWhenThumbnailKeyIsNull() {
            // given
            Long postId = 1L;
            Post post = createPost(postId);

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);

            // when
            postService.deletePost(postId);

            // then
            then(fileService).should(never()).deleteStorageObject(anyString());
            then(fileService).should().deleteFilesByPurpose(FilePurpose.ASSET, postId);
            assertThat(post.getThumbnailPurgeAt()).isNull();
        }

        @Test
        @DisplayName("연결 파일 S3 삭제 실패 시 예외가 발생한다")
        void deletePost_throwsExceptionWhenAssetFileDeleteFails() {
            // given
            Long postId = 1L;
            Post post = createPost(postId);

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);
            willThrow(new BusinessException(ErrorCode.STORAGE_DELETE_FAILED))
                    .given(fileService)
                    .deleteFilesByPurpose(FilePurpose.ASSET, postId);

            // when & then
            assertThatThrownBy(() -> postService.deletePost(postId))
                    .isInstanceOf(BusinessException.class);
            assertThat(post.getDeletedAt()).isNotNull();
        }

    }

    @Nested
    @DisplayName("게시글 수정 관련")
    class UpdatePost {

        PostUpdateRequest request;

        @BeforeEach
        void setUp(){
            request = TestUtil.postUpdateRequestOf();
        }

        @Test
        @DisplayName("게시글이 존재하면 제목, 내용, 카테고리, 태그를 수정한다")
        void updatePost_success() {
            // given
            Long postId = 1L;

            Post post = Post.builder()
                    .title("기존 제목")
                    .content("기존 내용")
                    .authorId(1L)
                    .categoryId(1L)
                    .linkedRequestId(null)
                    .build();

            Tag springTag = new Tag("spring");
            Tag jpaTag = new Tag("jpa");

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);

            given(tagService.findOrCreateAll(request.tags()))
                    .willReturn(new LinkedHashSet<>(List.of(springTag, jpaTag)));

            // when
            Post updatedPost = postService.updatePost(postId, request);

            // then
            assertThat(updatedPost.getTitle()).isEqualTo("수정 제목");
            assertThat(updatedPost.getContent()).isEqualTo("수정 내용");
            assertThat(updatedPost.getCategoryId()).isEqualTo(1L);

            then(postRepository)
                    .should()
                    .findByIdOrThrow(postId);

            then(tagService)
                    .should()
                    .findOrCreateAll(request.tags());
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND 예외를 발생시킨다")
        void updatePost_fail_when_post_not_found() {
            // given
            Long postId = 999L;

            given(postRepository.findByIdOrThrow(postId))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> postService.updatePost(postId, request))
                    .isInstanceOf(BusinessException.class);

            then(postRepository)
                    .should()
                    .findByIdOrThrow(postId);

            then(tagService)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("게시물 조회 관련")
    class postRead{
        @Nested
        @DisplayName("게시글 단건 조회")
        class GetPost {

            @Test
            @DisplayName("게시글이 존재하면 반환한다")
            void getPost_success() {
                // given
                Long postId = 1L;

                Post post = createPost(postId);

                given(postRepository.findByIdOrThrow(postId))
                        .willReturn(post);
                given(fileService.getShowPresignedUrl(post.getThumbnailKey()))
                        .willReturn("thumbnail-url");
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.ZIP))
                        .willReturn(List.of());
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                        .willReturn(List.of());

                // when
                PostReadResponse foundPost = postService.getPost(postId);

                // then
                assertThat(foundPost)
                        .extracting(
                                PostReadResponse::title,
                                PostReadResponse::content,
                                PostReadResponse::authorId,
                                PostReadResponse::categoryId
                        )
                        .containsExactly(
                                "제목",
                                "내용",
                                1L,
                                1L
                        );
                assertThat(foundPost.viewer()).isNull();
                assertThat(foundPost.downloadFile()).isNull();

                then(postRepository).should().findByIdOrThrow(postId);
            }

            @Test
            @DisplayName("MODEL과 TEXTURE가 있으면 게시글 상세 응답에 viewer가 포함된다")
            void getPost_returnsViewerWhenModelAndTextureExist() {
                // given
                Long postId = 10L;
                Post post = createPost(postId);
                FileAttachmentResponse zip = createAttachment(1L, "asset.zip", "zip-key", "https://zip-url", AssetFileType.ZIP, 1L);
                FileAttachmentResponse model = createAttachment(2L, "model.fbx", "model-key", "https://model-url", AssetFileType.MODEL, 2L);
                FileAttachmentResponse texture = createAttachment(3L, "basecolor.png", "texture-key", "https://texture-url", AssetFileType.TEXTURE, 3L);

                given(postRepository.findByIdOrThrow(postId))
                        .willReturn(post);
                given(fileService.getShowPresignedUrl(post.getThumbnailKey()))
                        .willReturn("thumbnail-url");
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.ZIP))
                        .willReturn(List.of(zip));
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                        .willReturn(List.of(model));
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.TEXTURE))
                        .willReturn(List.of(texture));

                // when
                PostReadResponse response = postService.getPost(postId);

                // then
                assertThat(response.id()).isEqualTo(postId);
                assertThat(response.title()).isEqualTo("제목");
                assertThat(response.files()).hasSize(1);
                assertThat(response.files().getFirst().fileType()).isEqualTo(AssetFileType.ZIP);
                assertThat(response.downloadFile()).isNotNull();
                assertThat(response.downloadFile().fileId()).isEqualTo(1L);
                assertThat(response.downloadFile().originalName()).isEqualTo("asset.zip");
                assertThat(response.downloadFile().fileType()).isEqualTo(AssetFileType.ZIP);
                assertThat(response.viewer()).isNotNull();
                assertThat(response.viewer().model().originalName()).isEqualTo("model.fbx");
                assertThat(response.viewer().model().accessUrl()).isEqualTo("https://model-url");
                assertThat(response.viewer().textures()).hasSize(1);
                assertThat(response.viewer().textures().getFirst().accessUrl()).isEqualTo("https://texture-url");
            }

            @Test
            @DisplayName("게시글 상세 조회 시 MODEL이 없으면 viewer는 null이다")
            void getPost_returnsNullViewerWhenModelDoesNotExist() {
                // given
                Long postId = 10L;
                Post post = createPost(postId);
                FileAttachmentResponse zip = createAttachment(1L, "asset.zip", "zip-key", "https://zip-url", AssetFileType.ZIP, 1L);

                given(postRepository.findByIdOrThrow(postId))
                        .willReturn(post);
                given(fileService.getShowPresignedUrl(post.getThumbnailKey()))
                        .willReturn("thumbnail-url");
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.ZIP))
                        .willReturn(List.of(zip));
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                        .willReturn(List.of());

                // when
                PostReadResponse response = postService.getPost(postId);

                // then
                assertThat(response.viewer()).isNull();
                assertThat(response.downloadFile()).isNotNull();
                assertThat(response.downloadFile().fileType()).isEqualTo(AssetFileType.ZIP);
                assertThat(response.files())
                        .extracting(FileAttachmentResponse::fileType)
                        .containsExactly(AssetFileType.ZIP);
                then(fileService).should(never()).getFileAttachmentsByPurposeAndFileType(
                        FilePurpose.ASSET,
                        postId,
                        AssetFileType.TEXTURE
                );
            }

            @Test
            @DisplayName("게시글 상세 조회 시 MODEL이 2개 이상이면 예외가 발생한다")
            void getPost_throwsExceptionWhenMultipleModelsExist() {
                // given
                Long postId = 10L;
                Post post = createPost(postId);
                FileAttachmentResponse firstModel = createAttachment(1L, "model.fbx", "model-key", "https://model-url", AssetFileType.MODEL, 2L);
                FileAttachmentResponse secondModel = createAttachment(2L, "other.fbx", "other-key", "https://other-url", AssetFileType.MODEL, 3L);

                given(postRepository.findByIdOrThrow(postId))
                        .willReturn(post);
                given(fileService.getShowPresignedUrl(post.getThumbnailKey()))
                        .willReturn("thumbnail-url");
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.ZIP))
                        .willReturn(List.of());
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                        .willReturn(List.of(firstModel, secondModel));

                // when & then
                assertThatThrownBy(() -> postService.getPost(postId))
                        .isInstanceOf(BusinessException.class);
            }

            @Test
            @DisplayName("게시글 상세 viewer에는 ZIP이 포함되지 않고 TEXTURE가 여러 개면 모두 포함된다")
            void getPost_returnsOnlyModelAndTexturesInViewer() {
                // given
                Long postId = 10L;
                Post post = createPost(postId);
                FileAttachmentResponse zip = createAttachment(1L, "asset.zip", "zip-key", "https://zip-url", AssetFileType.ZIP, 1L);
                FileAttachmentResponse model = createAttachment(2L, "model.fbx", "model-key", "https://model-url", AssetFileType.MODEL, 2L);
                FileAttachmentResponse baseColor = createAttachment(3L, "basecolor.png", "basecolor-key", "https://basecolor-url", AssetFileType.TEXTURE, 3L);
                FileAttachmentResponse normal = createAttachment(4L, "normal.png", "normal-key", "https://normal-url", AssetFileType.TEXTURE, 4L);

                given(postRepository.findByIdOrThrow(postId))
                        .willReturn(post);
                given(fileService.getShowPresignedUrl(post.getThumbnailKey()))
                        .willReturn("thumbnail-url");
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.ZIP))
                        .willReturn(List.of(zip));
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                        .willReturn(List.of(model));
                given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.TEXTURE))
                        .willReturn(List.of(baseColor, normal));

                // when
                PostReadResponse response = postService.getPost(postId);

                // then
                assertThat(response.files())
                        .extracting(FileAttachmentResponse::fileType)
                        .containsExactly(AssetFileType.ZIP);
                assertThat(response.downloadFile()).isNotNull();
                assertThat(response.downloadFile().fileId()).isEqualTo(1L);
                assertThat(response.viewer().model().fileType()).isEqualTo(AssetFileType.MODEL);
                assertThat(response.viewer().textures())
                        .extracting(PostViewerFileResponse::fileType)
                        .containsExactly(AssetFileType.TEXTURE, AssetFileType.TEXTURE);
                assertThat(response.viewer().textures())
                        .extracting(PostViewerFileResponse::originalName)
                        .containsExactly("basecolor.png", "normal.png");
            }

            @Test
            @DisplayName("게시글이 없으면 POST_NOT_FOUND 예외가 발생한다")
            void getPost_fail_when_not_found() {
                // given
                Long postId = 999L;

                given(postRepository.findByIdOrThrow(postId))
                        .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

                // when
                assertThatThrownBy(() -> postService.getPost(postId))
                        .isInstanceOf(BusinessException.class);
                //then
                then(postRepository).should().findByIdOrThrow(postId);
            }
        }

        @Nested
        @DisplayName("게시글 다건 조회")
        class GetPosts {
            @Test
            @DisplayName("삭제되지 않은 게시글 목록을 조회한다")
            void getPosts_success() {

                // given
                Pageable pageable = PageRequest.of(
                        0,
                        2,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                );

                List<Post> posts = List.of(
                        Post.builder()
                                .title("제목1")
                                .content("내용1")
                                .authorId(1L)
                                .categoryId(1L)
                                .build(),
                        Post.builder()
                                .title("제목2")
                                .content("내용2")
                                .authorId(2L)
                                .categoryId(1L)
                                .build()
                );

                Slice<Post> slice = new SliceImpl<>(
                        posts,
                        pageable,
                        true
                );

                given(postRepository.findAllByDeletedAtIsNull(pageable))
                        .willReturn(slice);
                given(fileService.getFileAttachmentsByPurposeAndFileType(eq(FilePurpose.ASSET), any(), eq(AssetFileType.ZIP)))
                        .willReturn(List.of());


                // when
                PostListResponse result = postService.getPosts(pageable);

                // then
                assertThat(result.items()).hasSize(2);
                assertThat(result.page()).isEqualTo(0);
                assertThat(result.size()).isEqualTo(2);
                assertThat(result.hasNext()).isTrue();

                assertThat(result.items())
                        .extracting(PostResponse::title)
                        .containsExactly("제목1", "제목2");

                then(postRepository)
                        .should()
                        .findAllByDeletedAtIsNull(pageable);
            }
        }
    }

    @Nested
    @DisplayName("게시글 미리보기 조회")
    class PostViewer {

        @Test
        @DisplayName("게시글이 존재하지 않으면 예외가 발생한다")
        void getPostViewer_fail_when_post_not_found() {
            // given
            Long postId = 999L;
            given(postRepository.findByIdOrThrow(postId))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> postService.getPostViewer(postId))
                    .isInstanceOf(BusinessException.class);

            then(postRepository).should().findByIdOrThrow(postId);
            then(fileService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("MODEL 파일이 없으면 예외가 발생한다")
        void getPostViewer_fail_when_model_not_found() {
            // given
            Long postId = 10L;
            Post post = createPost(postId);

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);
            given(fileService.getFileAttachmentsByPurposeAndFileType(
                    FilePurpose.ASSET,
                    postId,
                    AssetFileType.MODEL
            )).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> postService.getPostViewer(postId))
                    .isInstanceOf(BusinessException.class);

            then(fileService).should(never()).getFileAttachmentsByPurposeAndFileType(
                    FilePurpose.ASSET,
                    postId,
                    AssetFileType.TEXTURE
            );
        }

        @Test
        @DisplayName("/viewer API 호출 시 MODEL이 2개 이상이면 예외가 발생한다")
        void getPostViewer_fail_when_multiple_models_exist() {
            // given
            Long postId = 10L;
            Post post = createPost(postId);
            FileAttachmentResponse firstModel = createAttachment(1L, "model.fbx", "model-key", "https://model-url", AssetFileType.MODEL, 2L);
            FileAttachmentResponse secondModel = createAttachment(2L, "other.fbx", "other-key", "https://other-url", AssetFileType.MODEL, 3L);

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);
            given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                    .willReturn(List.of(firstModel, secondModel));

            // when & then
            assertThatThrownBy(() -> postService.getPostViewer(postId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("MODEL 파일이 정상 조회된다")
        void getPostViewer_returns_model() {
            // given
            Long postId = 10L;
            Post post = createPost(postId);
            FileAttachmentResponse model = createAttachment(
                    1L,
                    "model.fbx",
                    "posts/10/viewer/model/model.fbx",
                    "https://model-url",
                    AssetFileType.MODEL,
                    2L
            );

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);
            given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                    .willReturn(List.of(model));
            given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.TEXTURE))
                    .willReturn(List.of());

            // when
            PostViewerResponse response = postService.getPostViewer(postId);

            // then
            assertThat(response.postId()).isEqualTo(postId);
            assertThat(response.model().originalName()).isEqualTo("model.fbx");
            assertThat(response.model().fileType()).isEqualTo(AssetFileType.MODEL);
        }

        @Test
        @DisplayName("TEXTURE가 여러 개인 경우 모두 반환된다")
        void getPostViewer_returns_all_textures() {
            // given
            Long postId = 10L;
            Post post = createPost(postId);
            FileAttachmentResponse model = createAttachment(1L, "model.fbx", "model-key", "https://model-url", AssetFileType.MODEL, 2L);
            FileAttachmentResponse baseColor = createAttachment(2L, "basecolor.png", "basecolor-key", "https://basecolor-url", AssetFileType.TEXTURE, 3L);
            FileAttachmentResponse normal = createAttachment(3L, "normal.png", "normal-key", "https://normal-url", AssetFileType.TEXTURE, 4L);

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);
            given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                    .willReturn(List.of(model));
            given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.TEXTURE))
                    .willReturn(List.of(baseColor, normal));

            // when
            PostViewerResponse response = postService.getPostViewer(postId);

            // then
            assertThat(response.textures()).hasSize(2);
            assertThat(response.textures())
                    .extracting(PostViewerFileResponse::originalName)
                    .containsExactly("basecolor.png", "normal.png");
        }

        @Test
        @DisplayName("TEXTURE가 없어도 정상 응답한다")
        void getPostViewer_returns_empty_textures_when_texture_not_exists() {
            // given
            Long postId = 10L;
            Post post = createPost(postId);
            FileAttachmentResponse model = createAttachment(1L, "model.fbx", "model-key", "https://model-url", AssetFileType.MODEL, 2L);

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);
            given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                    .willReturn(List.of(model));
            given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.TEXTURE))
                    .willReturn(List.of());

            // when
            PostViewerResponse response = postService.getPostViewer(postId);

            // then
            assertThat(response.textures()).isEmpty();
        }

        @Test
        @DisplayName("accessUrl이 정상 생성되어 응답에 포함된다")
        void getPostViewer_returns_access_urls() {
            // given
            Long postId = 10L;
            Post post = createPost(postId);
            FileAttachmentResponse model = createAttachment(1L, "model.fbx", "model-key", "https://model-url", AssetFileType.MODEL, 2L);
            FileAttachmentResponse texture = createAttachment(2L, "basecolor.png", "texture-key", "https://texture-url", AssetFileType.TEXTURE, 3L);

            given(postRepository.findByIdOrThrow(postId))
                    .willReturn(post);
            given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.MODEL))
                    .willReturn(List.of(model));
            given(fileService.getFileAttachmentsByPurposeAndFileType(FilePurpose.ASSET, postId, AssetFileType.TEXTURE))
                    .willReturn(List.of(texture));

            // when
            PostViewerResponse response = postService.getPostViewer(postId);

            // then
            assertThat(response.model().accessUrl()).isEqualTo("https://model-url");
            assertThat(response.textures().getFirst().accessUrl()).isEqualTo("https://texture-url");
        }
    }

    private Post createPost(Long postId) {
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .authorId(1L)
                .categoryId(1L)
                .linkedRequestId(null)
                .build();
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }

    private FileAttachmentResponse createAttachment(
            Long fileId,
            String originalName,
            String s3Key,
            String accessUrl,
            AssetFileType fileType,
            Long uploadOrder
    ) {
        String extension = originalName.substring(originalName.lastIndexOf(".") + 1);
        return new FileAttachmentResponse(
                fileId,
                originalName,
                extension,
                s3Key,
                accessUrl,
                100L,
                fileType,
                uploadOrder
        );
    }

}
