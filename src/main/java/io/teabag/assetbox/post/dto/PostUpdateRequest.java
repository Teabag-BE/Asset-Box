package io.teabag.assetbox.post.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PostUpdateRequest(
        @NotBlank String title,
        @NotBlank String content,
        Long categoryId,
        List<String> tags) {
}
