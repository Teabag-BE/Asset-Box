package io.teabag.assetbox.request.dto;

import io.teabag.assetbox.file.dto.FileAttachmentResponse;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.domain.RequestStatus;
import java.time.LocalDateTime;
import java.util.List;

public record RequestResponse(
        Long id,
        String title,
        String content,
        String assetType,
        String preferredStyle,
        String engine,
        RequestStatus status,
        Long requesterId,
        Long assigneeId,
        Long linkedPostId,
        String thumbnailKey,
        String thumbnailUrl,
        LocalDateTime deadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<FileAttachmentResponse> referenceImages

) {


    public static RequestResponse from(RequestPost requestPost) {
        return from(requestPost, null, List.of());
    }

    public static RequestResponse from(RequestPost requestPost, List<FileAttachmentResponse> referenceImages)
    {
        return from(requestPost, null, referenceImages);
    }

    public static RequestResponse from(RequestPost requestPost,String thumbnailUrl)
    {
        return from(requestPost, thumbnailUrl, List.of());
    }

    public static RequestResponse from(RequestPost requestPost,
        String thumbnailUrl,
        List<FileAttachmentResponse> referenceImages
    ) {
        return new RequestResponse(
                requestPost.getId(),
                requestPost.getTitle(),
                requestPost.getContent(),
                requestPost.getAssetType(),
                requestPost.getPreferredStyle(),
                requestPost.getEngine(),
                requestPost.getStatus(),
                requestPost.getRequesterId(),
                requestPost.getAssigneeId(),
                requestPost.getLinkedPostId(),
                requestPost.getThumbnailKey() == null ? null : requestPost.getThumbnailKey(),
                thumbnailUrl,
                requestPost.getDeadline(),
                requestPost.getCreatedAt(),
                requestPost.getUpdatedAt(),
                referenceImages
        );
    }



}
