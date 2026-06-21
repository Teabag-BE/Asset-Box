package io.teabag.assetbox.comment.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CommentUpdateRequest(
        @NotBlank String content
) {
}
