package io.teabag.assetbox.request.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.request.domain.RequestPost;
import io.teabag.assetbox.request.dto.RequestCreateRequest;
import io.teabag.assetbox.request.service.RequestPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
        return ApiResponse.created(savedRequestPost,"요청글 생성 성공");
    }

    // 요청 목록
    @GetMapping
    public ApiResponse<Void> list() {
        return ApiResponse.ok();
    }
}