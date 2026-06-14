package io.teabag.assetbox.request.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.file.service.FileService;
import io.teabag.assetbox.util.TestUtil;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.domain.RequestStatus;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.dto.RequestListResponse;
import io.teabag.assetbox.request.dto.RequestResponse;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;



@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Transactional
class RequestPostServiceTests {

    @Mock
    RequestPostRepository requestPostRepository;

    @Mock
    FileService fileService;

    @InjectMocks
    RequestPostService requestPostService;


    @Nested
    @DisplayName("요청글 생성 관련")
    class requestCreate{

        @Test
        @DisplayName("생성 시 REQUESTED 상태로 게시글을 저장한다.")
        void saveRequestPost(){

            // given
            RequestCreateRequest request = TestUtil.requestCreateRequestOf();

            given(requestPostRepository.save(any(RequestPost.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            RequestResponse savedRequestPost = requestPostService.save(request, request.requesterId(), null);

            // then

            assertThat(savedRequestPost.title()).isEqualTo("요청 제목");
            assertThat(savedRequestPost.content()).isEqualTo("요청 내용");
            assertThat(savedRequestPost.assetType()).isEqualTo("CHARACTER");
            assertThat(savedRequestPost.preferredStyle()).isEqualTo("LOW_POLY");
            assertThat(savedRequestPost.engine()).isEqualTo("UNITY");
            assertThat(savedRequestPost.status()).isEqualTo(RequestStatus.REQUESTED);
            assertThat(savedRequestPost.requesterId()).isEqualTo(1L);

            then(requestPostRepository)
                    .should()
                    .save(any(RequestPost.class));

        }

        @Test
        @DisplayName("저장되는 요청글의 초기값이 올바르다")
        void saveRequest_initialValues() {
            // given
            RequestCreateRequest request = TestUtil.requestCreateRequestOf();

            given(requestPostRepository.save(any(RequestPost.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<RequestPost> captor = ArgumentCaptor.forClass(RequestPost.class);

            // when
            requestPostService.save(request, request.requesterId(), null);

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
    }
}
