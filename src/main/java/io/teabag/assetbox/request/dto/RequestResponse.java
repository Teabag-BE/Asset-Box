package io.teabag.assetbox.request.dto;

import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.domain.RequestStatus;
import java.time.LocalDateTime;

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
        LocalDateTime deadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static RequestResponse from(RequestPost requestPost) {
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
                requestPost.getDeadline(),
                requestPost.getCreatedAt(),
                requestPost.getUpdatedAt()
        );
    }
}