package io.teabag.assetbox.comment.dto;

import io.teabag.assetbox.post.dto.PostResponse;
import org.springframework.data.domain.Slice;

import java.util.List;

public record CommentListResponse(
        List<CommentResponse> comments,
        int page,
        int size,
        boolean hasNext
) {
    public static CommentListResponse from(Slice<CommentInfo> slice) {
        return new CommentListResponse(
                slice.getContent()
                        .stream()
                        .map(c -> CommentResponse.from(c))
                        .toList(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
