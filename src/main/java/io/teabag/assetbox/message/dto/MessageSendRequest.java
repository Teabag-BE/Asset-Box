package io.teabag.assetbox.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageSendRequest(
        @NotNull Long toUserId,
        @NotBlank @Size(max = 1000) String content
) {
}
