package io.teabag.assetbox.request.dto;

import org.springframework.data.domain.Slice;

import java.util.List;

public record RequestListResponse(
        List<RequestResponse> items,
        int page,
        int size,
        boolean hasNext
) {
    public static RequestListResponse fromResponses(Slice<RequestResponse> slice) {
        return new RequestListResponse(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
