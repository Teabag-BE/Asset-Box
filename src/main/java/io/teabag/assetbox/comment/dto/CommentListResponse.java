package io.teabag.assetbox.comment.dto;

import io.teabag.assetbox.comment.domain.Comment;
import io.teabag.assetbox.post.domain.Post;
import io.teabag.assetbox.post.dto.PostResponse;
import org.springframework.data.domain.Slice;

import java.util.List;

public record CommentListResponse(
        List<CommentResponse> comments,
        int page,
        int size,
        boolean hasNext
) {
    public static CommentListResponse from(Slice<Comment> slice) {
        return new CommentListResponse(
                slice.getContent()
                        .stream()
                        .map(CommentResponse::from)
                        .toList(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
