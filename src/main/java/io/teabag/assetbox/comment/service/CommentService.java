package io.teabag.assetbox.comment.service;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.comment.dto.CommentCreateRequest;
import io.teabag.assetbox.comment.dto.CommentResponse;
import io.teabag.assetbox.comment.repository.CommentRepository;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Transactional
    public CommentResponse createComment(Long postId, Long authorId, CommentCreateRequest request) {
        // Post 존재 검증
        postRepository.findByIdOrThrow(postId);

        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_PARENT_NOT_FOUND));

            // parent가 다른 post의 댓글인지 확인
            if (!parent.getPostId().equals(postId)) {
                throw new BusinessException(ErrorCode.COMMENT_PARENT_MISMATCH);
            }

            // parent가 이미 삭제됐는지 확인
            if (parent.isDeleted()) {
                throw new BusinessException(ErrorCode.COMMENT_PARENT_DELETED);
            }

            // 대댓글은 1단계까지만 허용 (parent가 이미 대댓글이면 불가)
            if (parent.getParentId() != null) {
                throw new BusinessException(ErrorCode.COMMENT_NESTED_TOO_DEEP);
            }
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .parentId(request.parentId())
                .content(request.content())
                .build();

        comment = commentRepository.save(comment);

        return toResponse(comment);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, Long authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // comment가 다른 post의 댓글인지 확인
        if (!comment.getPostId().equals(postId)) {
            throw new BusinessException(ErrorCode.COMMENT_POST_MISMATCH);
        }

        // 작성자 본인 확인
        if (!comment.getAuthorId().equals(authorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 이미 삭제된 댓글인지 확인
        if (comment.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED);
        }

        comment.softDelete();
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                comment.getParentId(),
                comment.getContent(),
                comment.isDeleted()
        );
    }
}