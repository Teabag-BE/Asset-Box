package io.teabag.assetbox.request.controller;

import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.dto.RequestListResponse;
import io.teabag.assetbox.request.dto.RequestResponse;
import io.teabag.assetbox.request.service.RequestPostService;
import io.teabag.assetbox.user.domain.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/requests")
public class RequestPostController {

    private final RequestPostService requestPostService;

    // 에셋 요청 작성
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RequestPost> saveRequest(
            @Valid @RequestBody RequestCreateRequest request
    ) {
        RequestPost savedRequestPost = requestPostService.save(request);
        return ApiResponse.created(savedRequestPost, "요청글 생성 성공");
    }

    // 요청 게시물 삭제
    @DeleteMapping("/{requestId}")
    public ApiResponse<Void> deleteRequestPost(
            @PathVariable Long requestId
    ){
        requestPostService.deleteRequestPost(requestId);

        return ApiResponse.ok(SuccessCode.REQUEST_DELETED.getSuccessMessage());

    }

    // assignee가 요청 수락
    @PatchMapping("/{requestId}/assign")
    public ApiResponse<RequestResponse> assignRequestPost(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CurrentUser currentUser
    ){
        RequestResponse response = requestPostService.assign(requestId, currentUser.getId());
        return ApiResponse.ok(response, "요청글 수락 성공");
    }


    // 요청글 다건 조회
    @GetMapping
    public ApiResponse<RequestListResponse> getRequestPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Slice<RequestPost> requestPosts = requestPostService.getRequests(pageable);

        return ApiResponse.ok(
                RequestListResponse.from(requestPosts),
                SuccessCode.REQUEST_READ.getSuccessMessage()
        );
    }

    // 요청글 단건 조회
    @GetMapping("/{requestId}")
    public ApiResponse<RequestResponse> getRequestPost(
            @PathVariable Long requestId
    ) {
        return ApiResponse.ok(
                RequestResponse.from(requestPostService.getRequest(requestId)),
                SuccessCode.REQUEST_READ_SINGLE.getSuccessMessage()
        );
    }
}
