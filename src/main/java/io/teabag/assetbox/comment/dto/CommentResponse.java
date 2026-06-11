package io.teabag.assetbox.comment.dto;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostResponse;

import java.util.List;

public record CommentResponse(
        Long id,
        Long postId,
        Long authorId,
        Long parentId,
        String content
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                comment.getParentId(),
                comment.getContent()
        );
    }
}
