package io.teabag.assetbox.request.dto;

import io.teabag.assetbox.request.domain.RequestPost;
import org.springframework.data.domain.Slice;

import java.util.List;

public record RequestListResponse(
        List<RequestResponse> items,
        int page,
        int size,
        boolean hasNext
) {
    public static RequestListResponse from(Slice<RequestPost> slice) {
        return new RequestListResponse(
                slice.getContent()
                        .stream()
                        .map(RequestResponse::from)
                        .toList(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
