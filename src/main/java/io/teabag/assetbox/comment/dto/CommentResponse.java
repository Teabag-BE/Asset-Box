package io.teabag.assetbox.comment.dto;

public record CommentResponse(Long id, Long postId, Long authorId, Long parentId, String content, boolean deleted) {
}
