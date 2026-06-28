package io.teabag.assetbox.comment.dto;

import io.teabag.assetbox.comment.domain.Comment;

import java.time.LocalDateTime;

public record CommentInfo(
        Long id,
        Long postId,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime deletedAt,
        Long parentId,
        LocalDateTime createdAt){

    public static CommentInfo from(Comment comment) {
        return new CommentInfo(
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



}
