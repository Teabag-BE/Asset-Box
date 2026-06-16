package io.teabag.assetbox.comment.dto;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostResponse;
import io.teabag.assetbox.user.domain.CurrentUser;
import io.teabag.assetbox.user.domain.User;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime deletedAt,
        Long parentId,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment, User user) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                user.getId(),
                user.getName(),
                comment.getContent(),
                comment.getDeletedAt(),
                comment.getParentId(),
                comment.getCreatedAt()
        );
    }

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                comment.getAuthorNickname(),
                comment.getContent(),
                comment.getDeletedAt(),
                comment.getParentId(),
                comment.getCreatedAt()
        );
    }

    public static CommentResponse from(CommentInfo comment) {
        return new CommentResponse(
                comment.id(),
                comment.postId(),
                comment.authorId(),
                comment.authorNickname(),
                comment.content(),
                comment.deletedAt(),
                comment.parentId(),
                comment.createdAt()
        );
    }
}
