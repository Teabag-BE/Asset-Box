package io.teabag.assetbox.feedback.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    @PostMapping
    public ApiResponse<Void> create() { return ApiResponse.ok(""); }
}
