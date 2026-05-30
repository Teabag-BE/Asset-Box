package io.teabag.assetbox.post.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PostCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        Long authorId,
        Long categoryId,
        List<String> tags,
        Long linkedRequestId) {
}
