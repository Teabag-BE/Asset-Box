package io.teabag.assetbox.request.dto;

import io.teabag.assetbox.request.domain.RequestComment;
import java.time.LocalDateTime;

// 프론트 CommentSection 이 쓰는 필드에 맞춤: id/parentId/content/authorId/authorNickname/createdAt/deletedAt.
public record RequestCommentResponse(
        Long id,
        Long requestId,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime deletedAt,
        Long parentId,
        LocalDateTime createdAt
) {
    public static RequestCommentResponse from(RequestComment c) {
        return new RequestCommentResponse(
                c.getId(),
                c.getRequestId(),
                c.getAuthorId(),
                c.getAuthorNickname(),
                c.getContent(),
                c.getDeletedAt(),
                c.getParentId(),
                c.getCreatedAt()
        );
    }
}
