package io.teabag.assetbox.feedback.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {
    @GetMapping
    public ApiResponse<Void> list() { return ApiResponse.ok(""); }
}
