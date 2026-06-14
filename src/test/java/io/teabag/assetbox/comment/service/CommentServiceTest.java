package io.teabag.assetbox.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.comment.dto.CommentResponse;
import io.teabag.assetbox.comment.repository.CommentRepository;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.repository.PostRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    CommentRepository commentRepository;

    @Mock
    PostRepository postRepository;

    @InjectMocks
    CommentService commentService;

    private void setCommentId(Comment comment, Long id) {
        ReflectionTestUtils.setField(comment, "id", id);
    }

    @Test
    @DisplayName("루트 댓글 작성 성공")
    void createComment_rootComment_returnsResponse() {
        // given
        Long postId = 1L;
        Long authorId = 10L;
        Post post = mock(Post.class);
        CommentCreateRequest request = new CommentCreateRequest("댓글 내용", null);

        given(postRepository.findByIdOrThrow(postId)).willReturn(post);
        given(commentRepository.save(any(Comment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        CommentResponse response = commentService.createComment(postId, authorId, request);

        // then
        assertThat(response.content()).isEqualTo("댓글 내용");
        assertThat(response.postId()).isEqualTo(postId);
        assertThat(response.parentId()).isNull();
        assertThat(response.deleted()).isFalse();
    }

    @Test
    @DisplayName("대댓글 작성 성공")
    void createComment_reply_returnsResponse() {
        // given
        Long postId = 1L;
        Long authorId = 10L;
        Post post = mock(Post.class);
        Comment parent = Comment.builder()
                .postId(postId)
                .authorId(20L)
                .content("부모 댓글")
                .build();
        setCommentId(parent, 100L);
        CommentCreateRequest request = new CommentCreateRequest("대댓글 내용", 100L);

        given(postRepository.findByIdOrThrow(postId)).willReturn(post);
        given(commentRepository.findById(100L)).willReturn(Optional.of(parent));
        given(commentRepository.save(any(Comment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        CommentResponse response = commentService.createComment(postId, authorId, request);

        // then
        assertThat(response.content()).isEqualTo("대댓글 내용");
        assertThat(response.parentId()).isEqualTo(parent.getId());
    }

    @Test
    @DisplayName("존재하지 않는 postId → POST_NOT_FOUND")
    void createComment_postNotFound_throws() {
        // given
        Long postId = 999L;
        given(postRepository.findByIdOrThrow(postId))
                .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        CommentCreateRequest request = new CommentCreateRequest("댓글", null);

        // when & then
        assertThatThrownBy(() -> commentService.createComment(postId, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("존재하지 않는 parentId → COMMENT_PARENT_NOT_FOUND")
    void createComment_parentNotFound_throws() {
        // given
        Long postId = 1L;
        Post post = mock(Post.class);
        given(postRepository.findByIdOrThrow(postId)).willReturn(post);
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        CommentCreateRequest request = new CommentCreateRequest("대댓글", 999L);

        // when & then
        assertThatThrownBy(() -> commentService.createComment(postId, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_PARENT_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("다른 post의 댓글을 parent로 지정 → COMMENT_PARENT_MISMATCH")
    void createComment_parentMismatch_throws() {
        // given
        Long postId = 1L;
        Long otherPostId = 2L;
        Post post = mock(Post.class);
        Comment parentOfOtherPost = Comment.builder()
                .postId(otherPostId)
                .authorId(20L)
                .content("다른 글의 댓글")
                .build();
        setCommentId(parentOfOtherPost, 100L);
        given(postRepository.findByIdOrThrow(postId)).willReturn(post);
        given(commentRepository.findById(100L))
                .willReturn(Optional.of(parentOfOtherPost));

        CommentCreateRequest request = new CommentCreateRequest("대댓글", 100L);

        // when & then
        assertThatThrownBy(() -> commentService.createComment(postId, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_PARENT_MISMATCH)
                );
    }

    @Test
    @DisplayName("2단계 대댓글 작성 → COMMENT_NESTED_TOO_DEEP")
    void createComment_nestedTooDeep_throws() {
        // given
        Long postId = 1L;
        Post post = mock(Post.class);
        Comment grandparent = Comment.builder()
                .postId(postId)
                .authorId(20L)
                .content("1단계 댓글")
                .build();
        setCommentId(grandparent, 100L);
        Comment parent = Comment.builder()
                .postId(postId)
                .authorId(30L)
                .parentId(100L)
                .content("2단계 댓글")
                .build();
        setCommentId(parent, 200L);
        given(postRepository.findByIdOrThrow(postId)).willReturn(post);
        given(commentRepository.findById(200L)).willReturn(Optional.of(parent));

        CommentCreateRequest request = new CommentCreateRequest("3단계 대댓글", 200L);

        // when & then
        assertThatThrownBy(() -> commentService.createComment(postId, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_NESTED_TOO_DEEP)
                );
    }

    @Test
    @DisplayName("삭제된 parent에 대댓글 작성 → COMMENT_PARENT_DELETED")
    void createComment_parentDeleted_throws() {
        // given
        Long postId = 1L;
        Post post = mock(Post.class);
        Comment deletedParent = Comment.builder()
                .postId(postId)
                .authorId(20L)
                .content("삭제된 부모 댓글")
                .build();
        setCommentId(deletedParent, 100L);
        deletedParent.softDelete();
        given(postRepository.findByIdOrThrow(postId)).willReturn(post);
        given(commentRepository.findById(100L))
                .willReturn(Optional.of(deletedParent));

        CommentCreateRequest request = new CommentCreateRequest("대댓글", 100L);

        // when & then
        assertThatThrownBy(() -> commentService.createComment(postId, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_PARENT_DELETED)
                );
    }

    @Test
    @DisplayName("본인 댓글 삭제 성공")
    void deleteComment_ownComment_succeeds() {
        // given
        Long postId = 1L;
        Long commentId = 100L;
        Long authorId = 10L;
        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .content("내 댓글")
                .build();
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        commentService.deleteComment(postId, commentId, authorId);

        // then
        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("다른 사용자 댓글 삭제 → FORBIDDEN")
    void deleteComment_otherUser_throws() {
        // given
        Long postId = 1L;
        Long commentId = 100L;
        Long commentAuthorId = 10L;
        Long requesterId = 20L;
        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(commentAuthorId)
                .content("다른 사람 댓글")
                .build();
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(postId, commentId, requesterId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
    }

    @Test
    @DisplayName("이미 삭제된 댓글 삭제 → COMMENT_ALREADY_DELETED")
    void deleteComment_alreadyDeleted_throws() {
        // given
        Long postId = 1L;
        Long commentId = 100L;
        Long authorId = 10L;
        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .content("이미 삭제된 댓글")
                .build();
        comment.softDelete();
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(postId, commentId, authorId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMENT_ALREADY_DELETED)
                );
    }
}
