package io.teabag.assetbox.request.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestPostTests {

    private RequestPost createRequestPost() {
        return RequestPost.builder()
                .title("요청 제목")
                .content("요청 내용")
                .assetType("CHARACTER")
                .preferredStyle("LOW_POLY")
                .engine("UNITY")
                .deadline(LocalDateTime.now().plusDays(7))
                .requesterId(1L)
                .build();
    }

    @Nested
    @DisplayName("요청글 생성")
    class CreateRequestPost {

        @Test
        @DisplayName("생성 시 필드와 기본 상태가 올바르게 설정된다")
        void createRequestPost_success() {
            // when
            RequestPost requestPost = createRequestPost();

            // then
            assertThat(requestPost.getTitle()).isEqualTo("요청 제목");
            assertThat(requestPost.getContent()).isEqualTo("요청 내용");
            assertThat(requestPost.getAssetType()).isEqualTo("CHARACTER");
            assertThat(requestPost.getPreferredStyle()).isEqualTo("LOW_POLY");
            assertThat(requestPost.getEngine()).isEqualTo("UNITY");
            assertThat(requestPost.getRequesterId()).isEqualTo(1L);
            assertThat(requestPost.getStatus()).isEqualTo(RequestStatus.REQUESTED);
            assertThat(requestPost.getAssigneeId()).isNull();
            assertThat(requestPost.getLinkedPostId()).isNull();
            assertThat(requestPost.getThumbnailKey()).isNull();
        }
    }

    @Nested
    @DisplayName("요청글 썸네일")
    class RequestPostThumbnail {

        @Test
        @DisplayName("thumbnailKey를 설정할 수 있다")
        void setThumbnailKey_success() {
            // given
            RequestPost requestPost = createRequestPost();

            // when
            requestPost.setThumbnailKey("thumbnail/reference/1/image.png");

            // then
            assertThat(requestPost.getThumbnailKey())
                    .isEqualTo("thumbnail/reference/1/image.png");
        }
    }

    @Nested
    @DisplayName("요청글 수락")
    class AssignRequestPost {

        @Test
        @DisplayName("assignee를 설정하고 상태를 IN_PROGRESS로 변경한다")
        void assign_success() {
            // given
            RequestPost requestPost = createRequestPost();

            // when
            requestPost.assign(2L);

            // then
            assertThat(requestPost.getAssigneeId()).isEqualTo(2L);
            assertThat(requestPost.getStatus()).isEqualTo(RequestStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("요청글 완료")
    class CompleteRequestPost {

        @Test
        @DisplayName("linkedPostId를 설정하고 상태를 COMPLETED로 변경한다")
        void complete_success() {
            // given
            RequestPost requestPost = createRequestPost();

            // when
            requestPost.complete(10L);

            // then
            assertThat(requestPost.getLinkedPostId()).isEqualTo(10L);
            assertThat(requestPost.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        }

        @Test
        @DisplayName("이미 linkedPostId가 있으면 다시 완료할 수 없다")
        void complete_fail_when_already_linked() {
            // given
            RequestPost requestPost = createRequestPost();
            requestPost.complete(10L);

            // when & then
            assertThatThrownBy(() -> requestPost.complete(20L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 연결된 게시글이 있습니다.");

            assertThat(requestPost.getLinkedPostId()).isEqualTo(10L);
            assertThat(requestPost.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("요청글 삭제")
    class DeleteRequestPost {

        @Test
        @DisplayName("softDelete 시 deletedAt을 설정한다")
        void softDelete_success() {
            // given
            RequestPost requestPost = createRequestPost();

            // when
            requestPost.softDelete();

            // then
            assertThat(requestPost.getDeletedAt()).isNotNull();
        }
    }
}
