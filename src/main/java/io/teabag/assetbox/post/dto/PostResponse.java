package io.teabag.assetbox.post.dto;

import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.domain.PostTag;

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
        List<PostFileInfo> files,
        List<String> tags,
        Long linkedRequestId
) {
    public static PostResponse from(Post post, String thumbnailUrl, FileUploadResponse fileResponse) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getCategoryId(),
                List.of(),
                post.getThumbnailKey() == null? null:post.getThumbnailKey(),
                thumbnailUrl,
                fileResponse.files().stream().map(PostFileInfo::from).toList(),
                post.getPostTags().stream()
                        .map(postTag -> postTag.getTag().getName())
                        .toList(),
                post.getLinkedRequestId()
        );
    }

    public static PostResponse from(Post post, String thumbnailUrl, List<FileAttachmentResponse> fileResponse) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getCategoryId(),
                List.of(),
                post.getThumbnailKey() == null? null:post.getThumbnailKey(),
                thumbnailUrl,
                fileResponse.stream().map(PostFileInfo::from).toList(),
                post.getPostTags().stream()
                        .map(postTag -> postTag.getTag().getName())
                        .toList(),
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
                post.files(),
                post.tags(),
                post.linkedRequestId()
        );
    }
}
