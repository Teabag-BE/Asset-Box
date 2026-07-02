package io.teabag.assetbox.request.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.domain.FilePurpose;
import io.teabag.assetbox.file.domain.ThumbnailPurpose;
import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.domain.RequestStatus;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.dto.RequestListResponse;
import io.teabag.assetbox.request.dto.RequestResponse;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import io.teabag.assetbox.user.constants.Major;
import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;
import io.teabag.assetbox.user.service.UserService;
import io.teabag.assetbox.util.TestUtil;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Transactional
class RequestPostServiceTests {

    @Mock
    RequestPostRepository requestPostRepository;

    @Mock
    UserService userService;

    @Mock
    FileService fileService;

    @InjectMocks
    RequestPostService requestPostService;

    private MockMultipartFile thumbnail() {
        return new MockMultipartFile(
                "thumbnail",
                "thumbnail.png",
                MediaType.IMAGE_PNG_VALUE,
                "thumbnail".getBytes()
        );
    }

    private MockMultipartFile emptyThumbnail() {
        return new MockMultipartFile(
                "thumbnail",
                "",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );
    }

    private MockMultipartFile emptyReferenceImage() {
        return new MockMultipartFile(
                "references",
                "",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );
    }

    private List<MultipartFile> referenceImages() {
        return List.of(
                new MockMultipartFile(
                        "references",
                        "reference-1.png",
                        MediaType.IMAGE_PNG_VALUE,
                        "reference-1".getBytes()
                ),
                new MockMultipartFile(
                        "references",
                        "reference-2.png",
                        MediaType.IMAGE_PNG_VALUE,
                        "reference-2".getBytes()
                )
        );
    }

    private void givenSavedRequestPostWithId(Long id) {
        given(requestPostRepository.save(any(RequestPost.class)))
                .willAnswer(invocation -> {
                    RequestPost requestPost = invocation.getArgument(0);
                    ReflectionTestUtils.setField(requestPost, "id", id);
                    return requestPost;
                });
    }


    private CurrentUser currentUser(Long id) {
        return currentUser(id, Major.BACK_END);
    }

    private CurrentUser currentUser(Long id, Major major) {
        return CurrentUser.builder()
                .id(id)
                .email("user@test.com")
                .name("user")
                .role(Role.USER)
                .major(major)
                .build();
    }

    private User user(Long id) {
        User user = User.builder()
                .email("user@test.com")
                .password("password")
                .name("user")
                .nickname("tester")
                .build();

        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Nested
    @DisplayName("요청글 생성 관련")
    class requestCreate{

        @Test
        @DisplayName("생성 시 REQUESTED 상태로 게시글을 저장한다.")
        void saveRequestPost(){


            // given
            RequestCreateRequest request = TestUtil.requestCreateRequestOf();
            CurrentUser currentUser = currentUser(1L);
            User user = user(1L);

            given(userService.currentUserToUser(currentUser)).willReturn(user);

            givenSavedRequestPostWithId(1L);
            given(fileService.uploadThumbnail(any(), any(), any()))
                    .willReturn("thumbnail/reference/1/thumbnail.png");
            given(fileService.getShowPresignedUrl("thumbnail/reference/1/thumbnail.png"))
                    .willReturn("https://cdn.test/thumbnail.png");
            given(fileService.getFileAttachmentsByPurpose(FilePurpose.REQUEST_REFERENCE, 1L))
                    .willReturn(List.of());

            // when
            RequestResponse savedRequestPost = requestPostService.save(
                    currentUser,
                    request,
                    thumbnail(),
                    null
            );

            // then

            assertThat(savedRequestPost.title()).isEqualTo("요청 제목");
            assertThat(savedRequestPost.content()).isEqualTo("요청 내용");
            assertThat(savedRequestPost.assetType()).isEqualTo("CHARACTER");
            assertThat(savedRequestPost.preferredStyle()).isEqualTo("LOW_POLY");
            assertThat(savedRequestPost.engine()).isEqualTo("UNITY");
            assertThat(savedRequestPost.status()).isEqualTo(RequestStatus.REQUESTED);
            assertThat(savedRequestPost.requesterId()).isEqualTo(1L);
            assertThat(savedRequestPost.thumbnailKey()).isEqualTo("thumbnail/reference/1/thumbnail.png");
            assertThat(savedRequestPost.thumbnailUrl()).isEqualTo("https://cdn.test/thumbnail.png");
            assertThat(savedRequestPost.referenceImages()).isEmpty();

            then(requestPostRepository)
                    .should()
                    .save(any(RequestPost.class));

            then(fileService)
                    .should()
                    .uploadThumbnail(any(), eq(ThumbnailPurpose.REFERENCE), eq(1L));

        }

        @Test
        @DisplayName("저장되는 요청글의 초기값이 올바르다")
        void saveRequest_initialValues() {
            // given
            RequestCreateRequest request = TestUtil.requestCreateRequestOf();
            CurrentUser currentUser = currentUser(1L);
            User user = user(1L);

            given(userService.currentUserToUser(currentUser)).willReturn(user);
            givenSavedRequestPostWithId(1L);
            given(fileService.uploadThumbnail(any(), any(), any()))
                    .willReturn("thumbnail/reference/1/thumbnail.png");
            given(fileService.getFileAttachmentsByPurpose(FilePurpose.REQUEST_REFERENCE, 1L))
                    .willReturn(List.of());

            ArgumentCaptor<RequestPost> captor = ArgumentCaptor.forClass(RequestPost.class);

            // when
            requestPostService.save(currentUser,request, thumbnail(), null);

            // then
            then(requestPostRepository)
                    .should()
                    .save(captor.capture());

            RequestPost savedRequestPost = captor.getValue();

            assertThat(savedRequestPost.getTitle()).isEqualTo("요청 제목");
            assertThat(savedRequestPost.getContent()).isEqualTo("요청 내용");
            assertThat(savedRequestPost.getAssetType()).isEqualTo("CHARACTER");
            assertThat(savedRequestPost.getPreferredStyle()).isEqualTo("LOW_POLY");
            assertThat(savedRequestPost.getEngine()).isEqualTo("UNITY");
            assertThat(savedRequestPost.getDeadline()).isEqualTo(request.deadline());
            assertThat(savedRequestPost.getRequesterId()).isEqualTo(1L);

            assertThat(savedRequestPost.getStatus()).isEqualTo(RequestStatus.REQUESTED);
            assertThat(savedRequestPost.getAssigneeId()).isNull();
            assertThat(savedRequestPost.getLinkedPostId()).isNull();
        }

        @Test
        @DisplayName("reference 이미지가 있으면 REQUEST_REFERENCE 파일로 업로드한다")
        void saveRequest_uploadReferences() {
            // given
            RequestCreateRequest request = TestUtil.requestCreateRequestOf();
            List<MultipartFile> references = referenceImages();
            CurrentUser currentUser = currentUser(1L);
            User user = user(1L);

            given(userService.currentUserToUser(currentUser)).willReturn(user);

            givenSavedRequestPostWithId(1L);
            given(fileService.uploadThumbnail(any(), any(), any()))
                    .willReturn("thumbnail/reference/1/thumbnail.png");
            given(fileService.getShowPresignedUrl("thumbnail/reference/1/thumbnail.png"))
                    .willReturn("https://cdn.test/thumbnail.png");

            List<FileAttachmentResponse> attachments = List.of(
                    new FileAttachmentResponse(
                            10L,
                            "reference-1.png",
                            "png",
                            "files/request/1/reference-1.png",
                            "https://cdn.test/reference-1.png",
                            1000L,
                            null,
                            1L
                    )
            );
            given(fileService.getFileAttachmentsByPurpose(FilePurpose.REQUEST_REFERENCE, 1L))
                    .willReturn(attachments);


            // when
            RequestResponse response = requestPostService.save(
                    currentUser,
                    request,
                    thumbnail(),
                    references
            );

            // then
            assertThat(response.referenceImages()).hasSize(1);
            assertThat(response.referenceImages().get(0).fileId()).isEqualTo(10L);

            then(fileService)
                    .should()
                    .uploadFiles(eq(references), eq(FilePurpose.REQUEST_REFERENCE),
                            eq(1L),any(UUID.class),eq(user));

        }

        @Test
        @DisplayName("thumbnail과 reference 이미지가 없어도 요청글을 생성한다")
        void saveRequest_withoutFiles() {
            // given
            RequestCreateRequest request = TestUtil.requestCreateRequestOf();
            CurrentUser currentUser = currentUser(1L);
            User user = user(1L);

            given(userService.currentUserToUser(currentUser)).willReturn(user);
            givenSavedRequestPostWithId(1L);
            given(fileService.getFileAttachmentsByPurpose(FilePurpose.REQUEST_REFERENCE, 1L))
                    .willReturn(List.of());

            // when
            RequestResponse response = requestPostService.save(
                    currentUser,
                    request,
                    null,
                    null
            );

            // then
            assertThat(response.title()).isEqualTo("요청 제목");
            assertThat(response.thumbnailKey()).isNull();
            assertThat(response.thumbnailUrl()).isNull();
            assertThat(response.referenceImages()).isEmpty();

            then(fileService)
                    .should(never())
                    .uploadThumbnail(any(), any(), any());
            then(fileService)
                    .should(never())
                    .uploadFiles(any(), any(), any(), any(), any());
            then(fileService)
                    .should(never())
                    .getShowPresignedUrl(anyString());
        }

        @Test
        @DisplayName("thumbnail 없이 reference 이미지만 있어도 요청글을 생성하고 reference 이미지를 업로드한다")
        void saveRequest_withReferencesOnly() {
            // given
            RequestCreateRequest request = TestUtil.requestCreateRequestOf();
            List<MultipartFile> references = referenceImages();
            CurrentUser currentUser = currentUser(1L);
            User user = user(1L);

            given(userService.currentUserToUser(currentUser)).willReturn(user);
            givenSavedRequestPostWithId(1L);

            List<FileAttachmentResponse> attachments = List.of(
                    new FileAttachmentResponse(
                            10L,
                            "reference-1.png",
                            "png",
                            "files/request/1/reference-1.png",
                            "https://cdn.test/reference-1.png",
                            1000L,
                            null,
                            1L
                    )
            );
            given(fileService.getFileAttachmentsByPurpose(FilePurpose.REQUEST_REFERENCE, 1L))
                    .willReturn(attachments);

            // when
            RequestResponse response = requestPostService.save(
                    currentUser,
                    request,
                    null,
                    references
            );

            // then
            assertThat(response.thumbnailKey()).isNull();
            assertThat(response.thumbnailUrl()).isNull();
            assertThat(response.referenceImages()).hasSize(1);

            then(fileService)
                    .should(never())
                    .uploadThumbnail(any(), any(), any());
            then(fileService)
                    .should()
                    .uploadFiles(eq(references), eq(FilePurpose.REQUEST_REFERENCE),
                            eq(1L), any(UUID.class), eq(user));
        }

        @Test
        @DisplayName("빈 파일 파트는 업로드하지 않고 요청글을 생성한다")
        void saveRequest_ignoreEmptyFileParts() {
            // given
            RequestCreateRequest request = TestUtil.requestCreateRequestOf();
            CurrentUser currentUser = currentUser(1L);
            User user = user(1L);

            given(userService.currentUserToUser(currentUser)).willReturn(user);
            givenSavedRequestPostWithId(1L);
            given(fileService.getFileAttachmentsByPurpose(FilePurpose.REQUEST_REFERENCE, 1L))
                    .willReturn(List.of());

            // when
            RequestResponse response = requestPostService.save(
                    currentUser,
                    request,
                    emptyThumbnail(),
                    List.of(emptyReferenceImage())
            );

            // then
            assertThat(response.thumbnailKey()).isNull();
            assertThat(response.thumbnailUrl()).isNull();
            assertThat(response.referenceImages()).isEmpty();

            then(fileService)
                    .should(never())
                    .uploadThumbnail(any(), any(), any());
            then(fileService)
                    .should(never())
                    .uploadFiles(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("요청글 삭제 관련")
    class requestPostDelete{
        @Test
        @DisplayName("삭제 시 실제 삭제하지 않고 deleteAt을 채운다.")
        void deleteRequestPost_success(){
            // given
            Long requestPostId = 1L;

            RequestPost requestPost =RequestPost.builder()
                    .title("제목")
                    .content("내용")
                    .assetType("CHARACTER")
                    .preferredStyle("LOW_POLY")
                    .engine("UNITY")
                    .deadline(TestUtil.requestCreateRequestOf().deadline())
                    .requesterId(1L)
                    .build();

            given(requestPostRepository.findByIdOrThrow(requestPostId))
                    .willReturn(requestPost);

            // when
            requestPostService.deleteRequestPost(requestPostId);

            // then
            assertThat(requestPost.getDeletedAt()).isNotNull();

            then(requestPostRepository)
                    .should()
                    .findByIdOrThrow(requestPostId);

            then(requestPostRepository)
                    .should(never())
                    .delete(any(RequestPost.class));
        }

        @Test
        @DisplayName("REQUESTED 상태가 아니면 REQUEST_NOT_DELETABLE 예외가 발생한다")
        void deleteRequestPost_fail_when_status_is_not_requested() {
            // given
            Long requestPostId = 1L;

            RequestPost requestPost = RequestPost.builder()
                    .title("제목")
                    .content("내용")
                    .assetType("CHARACTER")
                    .preferredStyle("LOW_POLY")
                    .engine("UNITY")
                    .deadline(TestUtil.requestCreateRequestOf().deadline())
                    .requesterId(1L)
                    .build();

            ReflectionTestUtils.setField(requestPost, "status", RequestStatus.IN_PROGRESS);

            given(requestPostRepository.findByIdOrThrow(requestPostId))
                    .willReturn(requestPost);

            // when & then
            assertThatThrownBy(() -> requestPostService.deleteRequestPost(requestPostId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.REQUEST_NOT_DELETABLE.getDescription());

            assertThat(requestPost.getDeletedAt()).isNull();

            then(requestPostRepository)
                    .should()
                    .findByIdOrThrow(requestPostId);

            then(requestPostRepository)
                    .should(never())
                    .delete(any(RequestPost.class));
        }

        @Test
        @DisplayName("존재하지 않는 요청글 삭제 시 REQUEST_NOT_FOUND 예외가 발생한다")
        void deleteRequestPost_fail_when_request_not_found() {
            // given
            Long requestPostId = 999L;

            given(requestPostRepository.findByIdOrThrow(requestPostId))
                    .willThrow(new BusinessException(ErrorCode.REQUEST_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> requestPostService.deleteRequestPost(requestPostId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.REQUEST_NOT_FOUND.getDescription());

            then(requestPostRepository)
                    .should()
                    .findByIdOrThrow(requestPostId);

            then(requestPostRepository)
                    .should(never())
                    .delete(any(RequestPost.class));
        }
    }

    @Nested
    @DisplayName("요청글 조회 관련")
    class requestRead {

        @Nested
        @DisplayName("요청글 단건 조회")
        class GetRequest {

            @Test
            @DisplayName("요청글이 존재하면 반환한다")
            void getRequest_success() {
                // given
                Long requestId = 1L;
                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(TestUtil.requestCreateRequestOf().deadline())
                        .requesterId(1L)
                        .build();

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willReturn(requestPost);
                given(fileService.getFileAttachmentsByPurpose(FilePurpose.REQUEST_REFERENCE, null))
                        .willReturn(List.of());

                // when
                RequestResponse foundRequestPost = requestPostService.getRequest(requestId);

                // then
                assertThat(foundRequestPost.title()).isEqualTo("요청 제목");
                assertThat(foundRequestPost.status()).isEqualTo(RequestStatus.REQUESTED);

                then(requestPostRepository)
                        .should()
                        .findByIdOrThrow(requestId);
            }

            @Test
            @DisplayName("요청글 조회 시 thumbnailUrl과 referenceImages를 함께 반환한다")
            void getRequest_withAttachments_success() {
                // given
                Long requestId = 1L;
                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(TestUtil.requestCreateRequestOf().deadline())
                        .requesterId(1L)
                        .build();
                ReflectionTestUtils.setField(requestPost, "id", requestId);
                requestPost.setThumbnailKey("thumbnail/reference/1/thumbnail.png");

                List<FileAttachmentResponse> attachments = List.of(
                        new FileAttachmentResponse(
                                10L,
                                "reference-1.png",
                                "png",
                                "files/request/1/reference-1.png",
                                "https://cdn.test/reference-1.png",
                                1000L,
                                null,
                                1L
                        )
                );

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willReturn(requestPost);
                given(fileService.getFileAttachmentsByPurpose(FilePurpose.REQUEST_REFERENCE, requestId))
                        .willReturn(attachments);
                given(fileService.getShowPresignedUrl("thumbnail/reference/1/thumbnail.png"))
                        .willReturn("https://cdn.test/thumbnail.png");

                // when
                RequestResponse response = requestPostService.getRequest(requestId);

                // then
                assertThat(response.thumbnailKey()).isEqualTo("thumbnail/reference/1/thumbnail.png");
                assertThat(response.thumbnailUrl()).isEqualTo("https://cdn.test/thumbnail.png");
                assertThat(response.referenceImages()).hasSize(1);
                assertThat(response.referenceImages().get(0).fileId()).isEqualTo(10L);

                then(fileService)
                        .should()
                        .getFileAttachmentsByPurpose(FilePurpose.REQUEST_REFERENCE, requestId);
                then(fileService)
                        .should()
                        .getShowPresignedUrl("thumbnail/reference/1/thumbnail.png");
            }

            @Test
            @DisplayName("요청글이 없으면 REQUEST_NOT_FOUND 예외가 발생한다")
            void getRequest_fail_when_not_found() {
                // given
                Long requestId = 999L;

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willThrow(new BusinessException(ErrorCode.REQUEST_NOT_FOUND));

                // when & then
                assertThatThrownBy(() -> requestPostService.getRequest(requestId))
                        .isInstanceOf(BusinessException.class);

                then(requestPostRepository)
                        .should()
                        .findByIdOrThrow(requestId);
            }
        }

        @Nested
        @DisplayName("요청글 다건 조회")
        class GetRequests {

            @Test
            @DisplayName("삭제되지 않은 요청글 목록을 조회한다")
            void getRequests_success() {
                // given
                Pageable pageable = PageRequest.of(
                        0,
                        2,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                );

                List<RequestPost> requestPosts = List.of(
                        RequestPost.builder()
                                .title("요청 제목1")
                                .content("요청 내용1")
                                .assetType("CHARACTER")
                                .preferredStyle("LOW_POLY")
                                .engine("UNITY")
                                .deadline(TestUtil.requestCreateRequestOf().deadline())
                                .requesterId(1L)
                                .build(),
                        RequestPost.builder()
                                .title("요청 제목2")
                                .content("요청 내용2")
                                .assetType("PROP")
                                .preferredStyle("REALISTIC")
                                .engine("UNREAL")
                                .deadline(TestUtil.requestCreateRequestOf().deadline())
                                .requesterId(2L)
                                .build()
                );

                Slice<RequestPost> slice = new SliceImpl<>(
                        requestPosts,
                        pageable,
                        true
                );

                given(requestPostRepository.findAllByDeletedAtIsNull(pageable))
                        .willReturn(slice);

                // when
                RequestListResponse result = requestPostService.getRequests(pageable);

                // then
                assertThat(result.items()).hasSize(2);
                assertThat(result.page()).isEqualTo(0);
                assertThat(result.size()).isEqualTo(2);
                assertThat(result.hasNext()).isTrue();

                assertThat(result.items())
                        .extracting(RequestResponse::title)
                        .containsExactly("요청 제목1", "요청 제목2");

                then(requestPostRepository)
                        .should()
                        .findAllByDeletedAtIsNull(pageable);
            }
        }

        @Nested
        @DisplayName("요청글 수락")
        class AssignRequest {

            @Test
            @DisplayName("REQUESTED 상태 요청글을 수락하면 assignee와 상태를 변경한다")
            void assign_success() {
                // given
                Long requestId = 1L;
                Long assigneeId = 2L;
                CurrentUser assignee = currentUser(assigneeId, Major.TA);
                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(TestUtil.requestCreateRequestOf().deadline())
                        .requesterId(1L)
                        .build();

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willReturn(requestPost);

                // when
                RequestResponse response = requestPostService.assign(requestId, assignee);

                // then
                assertThat(response.assigneeId()).isEqualTo(assigneeId);
                assertThat(response.status()).isEqualTo(RequestStatus.IN_PROGRESS);
                assertThat(requestPost.getAssigneeId()).isEqualTo(assigneeId);
                assertThat(requestPost.getStatus()).isEqualTo(RequestStatus.IN_PROGRESS);
            }

            @Test
            @DisplayName("이미 본인이 수락한 요청글이면 REQUEST_ASSIGN_SELF_DUPLICATED 예외가 발생한다")
            void assign_fail_when_same_assignee() {
                // given
                Long requestId = 1L;
                Long assigneeId = 2L;
                CurrentUser assignee = currentUser(assigneeId, Major.TA);
                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(TestUtil.requestCreateRequestOf().deadline())
                        .requesterId(1L)
                        .build();
                requestPost.assign(assigneeId);

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willReturn(requestPost);

                // when & then
                assertThatThrownBy(() -> requestPostService.assign(requestId, assignee))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.REQUEST_ASSIGN_SELF_DUPLICATED.getDescription());
            }

            @Test
            @DisplayName("다른 담당자가 이미 수락한 요청글이면 REQUEST_ASSIGN_TAKEN 예외가 발생한다")
            void assign_fail_when_other_assignee_exists() {
                // given
                Long requestId = 1L;
                CurrentUser assignee = currentUser(2L, Major.TA);
                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(TestUtil.requestCreateRequestOf().deadline())
                        .requesterId(1L)
                        .build();
                requestPost.assign(3L);

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willReturn(requestPost);

                // when & then
                assertThatThrownBy(() -> requestPostService.assign(requestId, assignee))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.REQUEST_ASSIGN_TAKEN.getDescription());
            }

            @Test
            @DisplayName("요청글 작성자가 본인 요청글을 수락하면 REQUEST_ASSIGN_REQUESTER_NOT_ALLOWED 예외가 발생한다")
            void assign_fail_when_requester_assigns_own_request() {
                // given
                Long requestId = 1L;
                Long requesterId = 1L;
                CurrentUser requester = currentUser(requesterId, Major.TA);
                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(TestUtil.requestCreateRequestOf().deadline())
                        .requesterId(requesterId)
                        .build();

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willReturn(requestPost);

                // when & then
                assertThatThrownBy(() -> requestPostService.assign(requestId, requester))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.REQUEST_ASSIGN_REQUESTER_NOT_ALLOWED.getDescription());
            }

            @Test
            @DisplayName("TA 전공이 아니면 REQUEST_ASSIGN_FORBIDDEN 예외가 발생한다")
            void assign_fail_when_user_major_is_not_ta() {
                // given
                Long requestId = 1L;
                CurrentUser assignee = currentUser(2L, Major.BACK_END);

                // when & then
                assertThatThrownBy(() -> requestPostService.assign(requestId, assignee))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.REQUEST_ASSIGN_FORBIDDEN.getDescription());

                then(requestPostRepository)
                        .should(never())
                        .findByIdOrThrow(anyLong());
            }
        }

        @Nested
        @DisplayName("요청글 완료")
        class CompleteRequest {

            @Test
            @DisplayName("IN_PROGRESS 상태 요청글을 완료하면 linkedPostId와 상태를 변경한다")
            void completeByLinkedPost_success() {
                // given
                Long requestId = 1L;
                Long assigneeId = 2L;
                Long linkedPostId = 10L;
                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(TestUtil.requestCreateRequestOf().deadline())
                        .requesterId(1L)
                        .build();
                requestPost.assign(assigneeId);

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willReturn(requestPost);

                // when
                RequestResponse response = requestPostService.completeByLinkedPost(
                        requestId,
                        assigneeId,
                        linkedPostId
                );

                // then
                assertThat(response.linkedPostId()).isEqualTo(linkedPostId);
                assertThat(response.status()).isEqualTo(RequestStatus.COMPLETED);
                assertThat(requestPost.getLinkedPostId()).isEqualTo(linkedPostId);
                assertThat(requestPost.getStatus()).isEqualTo(RequestStatus.COMPLETED);
            }

            @Test
            @DisplayName("담당자가 아니면 REQUEST_ASSIGNEE_MISMATCH 예외가 발생한다")
            void completeByLinkedPost_fail_when_assignee_mismatch() {
                // given
                Long requestId = 1L;
                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(TestUtil.requestCreateRequestOf().deadline())
                        .requesterId(1L)
                        .build();
                requestPost.assign(2L);

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willReturn(requestPost);

                // when & then
                assertThatThrownBy(() -> requestPostService.completeByLinkedPost(requestId, 3L, 10L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.REQUEST_ASSIGNEE_MISMATCH.getDescription());
            }

            @Test
            @DisplayName("IN_PROGRESS 상태가 아니면 POST_LINKED_REQUEST_INVALID_STATUS 예외가 발생한다")
            void completeByLinkedPost_fail_when_status_is_not_in_progress() {
                // given
                Long requestId = 1L;
                Long assigneeId = 2L;
                RequestPost requestPost = RequestPost.builder()
                        .title("요청 제목")
                        .content("요청 내용")
                        .assetType("CHARACTER")
                        .preferredStyle("LOW_POLY")
                        .engine("UNITY")
                        .deadline(TestUtil.requestCreateRequestOf().deadline())
                        .requesterId(1L)
                        .build();
                ReflectionTestUtils.setField(requestPost, "assigneeId", assigneeId);

                given(requestPostRepository.findByIdOrThrow(requestId))
                        .willReturn(requestPost);

                // when & then
                assertThatThrownBy(() -> requestPostService.completeByLinkedPost(requestId, assigneeId, 10L))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining(ErrorCode.POST_LINKED_REQUEST_INVALID_STATUS.getDescription());
            }
        }
    }
}
