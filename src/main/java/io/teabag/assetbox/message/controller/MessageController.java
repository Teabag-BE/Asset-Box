package io.teabag.assetbox.message.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    @GetMapping
    public ApiResponse<Void> inbox() { return ApiResponse.ok(); }
}
