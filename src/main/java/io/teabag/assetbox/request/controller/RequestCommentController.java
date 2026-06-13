package io.teabag.assetbox.request.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests/{requestId}/comments")
public class RequestCommentController {
    @GetMapping
    public ApiResponse<Void> list() { return ApiResponse.ok(""); }
}
