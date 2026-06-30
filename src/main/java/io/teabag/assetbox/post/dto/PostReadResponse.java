package io.teabag.assetbox.post.dto;

import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.file.dto.FileUploadResponse;
import io.teabag.assetbox.post.domain.Post;

import java.util.List;

public record PostReadResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        Long categoryId,
        List<String> categoryPath,
        String thumbnailKey,
        String thumbnailUrl,
        List<FileAttachmentResponse> files,
        List<String> tags,
        Long linkedRequestId
) {


    public static PostReadResponse from(Post post, String thumbnailUrl, List<FileAttachmentResponse> fileResponse) {
        return new PostReadResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getCategoryId(),
                List.of(),
                post.getThumbnailKey() == null? null:post.getThumbnailKey(),
                thumbnailUrl,
                fileResponse,
                post.getPostTags().stream()
                        .map(postTag -> postTag.getTag().getName())
                        .toList(),
                post.getLinkedRequestId()
        );
    }
}
