package io.teabag.assetbox.request.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.teabag.assetbox.common.constants.SuccessCode;
import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.dto.RequestListResponse;
import io.teabag.assetbox.request.dto.RequestResponse;
import io.teabag.assetbox.request.service.RequestPostService;
import io.teabag.assetbox.user.domain.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name="bearerAuth")
@RequestMapping("/api/requests")
public class RequestPostController {

    private final RequestPostService requestPostService;

    // 에셋 요청 작성
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RequestResponse> saveRequest(
            @Valid @RequestPart("request") RequestCreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "references", required = false) List<MultipartFile> references,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
//        Long requesterId = currentUser.getId();
//        RequestResponse savedRequestPost = requestPostService.save(request, requesterId, thumbnail,references);
        RequestResponse savedRequestPost = requestPostService.save(currentUser, request, thumbnail, references);


        return ApiResponse.created(savedRequestPost, SuccessCode.REQUEST_CREATED.getSuccessMessage());
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
        RequestResponse response = requestPostService.assign(requestId, currentUser);
        return ApiResponse.ok(response, "요청글 수락 성공");
    }


    // 요청글 다건 조회
    @GetMapping
    public ApiResponse<RequestListResponse> getRequestPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        RequestListResponse requestPosts = requestPostService.getRequests(pageable);

        return ApiResponse.ok(
                requestPosts,
                SuccessCode.REQUEST_READ.getSuccessMessage()
        );
    }

    // 요청글 단건 조회
    @GetMapping("/{requestId}")
    public ApiResponse<RequestResponse> getRequestPost(
            @PathVariable Long requestId
    ) {
        return ApiResponse.ok(
                requestPostService.getRequest(requestId),
                SuccessCode.REQUEST_READ_SINGLE.getSuccessMessage()
        );
    }
}
