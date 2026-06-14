package io.teabag.assetbox.comment.dto;

import io.teabag.assetbox.comment.domain.Comment;


import java.time.LocalDateTime;
import java.util.List;

public record CommentInfo(
        Long id,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime deletedAt,
        Long parentId ,
        LocalDateTime createdAt){

    public static CommentInfo from(Comment comment) {

        return new CommentInfo(
                comment.getId(),
                comment.getAuthorId(),
                comment.getAuthorNickname(),
                comment.getContent(),
                comment.getDeletedAt(),
                comment.getParentId(),
                comment.getCreatedAt());
    }


}
