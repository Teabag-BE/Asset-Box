package io.teabag.assetbox.request.dto;

import io.teabag.assetbox.request.domain.RequestComment;
import java.util.List;
import org.springframework.data.domain.Slice;

// 프론트 requestCommentApi 는 응답 data 의 comments 배열을 언랩한다.
public record RequestCommentListResponse(
        List<RequestCommentResponse> comments,
        int page,
        int size,
        boolean hasNext
) {
    public static RequestCommentListResponse from(Slice<RequestComment> slice) {
        return new RequestCommentListResponse(
                slice.getContent().stream().map(RequestCommentResponse::from).toList(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
