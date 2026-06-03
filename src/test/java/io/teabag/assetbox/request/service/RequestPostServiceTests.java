package io.teabag.assetbox.request.service;

import io.teabag.assetbox.util.TestUtil;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.domain.RequestStatus;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.repository.RequestPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.any;



@ExtendWith(MockitoExtension.class)
@Transactional
class RequestPostServiceTests {

    @Mock
    RequestPostRepository requestPostRepository;

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
            RequestPost savedRequestPost = requestPostService.save(request);

            // then

            assertThat(savedRequestPost.getTitle()).isEqualTo("요청 제목");
            assertThat(savedRequestPost.getContent()).isEqualTo("요청 내용");
            assertThat(savedRequestPost.getAssetType()).isEqualTo("CHARACTER");
            assertThat(savedRequestPost.getPreferredStyle()).isEqualTo("LOW_POLY");
            assertThat(savedRequestPost.getEngine()).isEqualTo("UNITY");
            assertThat(savedRequestPost.getStatus()).isEqualTo(RequestStatus.REQUESTED);
            assertThat(savedRequestPost.getRequesterId()).isEqualTo(1L);

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
            requestPostService.save(request);

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
}