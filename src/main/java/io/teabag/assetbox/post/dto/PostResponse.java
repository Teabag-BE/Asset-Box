package io.teabag.assetbox.post.dto;

import io.teabag.assetbox.post.domain.Post;

import java.util.List;

public record PostResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        Long categoryId,
        List<String> categoryPath,
        Long thumbnailFileId,
        String thumbnailUrl,
        List<String> tags,
        Long linkedRequestId
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getCategoryId(),
                List.of(),
                null,
                null,
                List.of(),
                post.getLinkedRequestId()
        );
    }
}
