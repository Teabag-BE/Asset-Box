package io.teabag.assetbox.post.dto;

import io.teabag.assetbox.post.domain.Post;
import org.springframework.data.domain.Slice;

import java.util.List;

public record PostListResponse(
        List<PostResponse> items,
        int page,
        int size,
        boolean hasNext
) {
    public static PostListResponse from(Slice<PostInfo> slice) {
        return new PostListResponse(
                slice.getContent()
                        .stream()
                        .map(p -> PostResponse.from(p))
                        .toList(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
