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
        String thumbnailKey,
        String thumbnailUrl,
        List<String> tags,
        Long linkedRequestId
) {
    public static PostResponse from(Post post, String thumbnailUrl) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getCategoryId(),
                List.of(),
                post.getThumbnailKey() == null? null:post.getThumbnailKey(),
                thumbnailUrl,
                List.of(),
                post.getLinkedRequestId()
        );
    }

    public static PostResponse from(PostInfo post) {
        return new PostResponse(
                post.id(),
                post.title(),
                post.content(),
                post.authorId(),
                post.categoryId(),
                List.of(),
                post.thumbnailKey() == null? null:post.thumbnailKey(),
                post.thumbnailUrl(),
                List.of(),
                post.linkedRequestId()
        );
    }
}
