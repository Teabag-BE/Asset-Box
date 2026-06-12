package io.teabag.assetbox.user.controller;

import io.teabag.assetbox.common.dto.ApiResponse;
import io.teabag.assetbox.user.dto.Paging;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @GetMapping
    public ApiResponse<Void> list(
            @Valid Paging paging,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String q
    ) {
        return ApiResponse.ok();
    }
}
