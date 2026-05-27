package io.teabag.assetbox.comment.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    @GetMapping
    public ApiResponse<Void> list() { return ApiResponse.ok(); }
}
