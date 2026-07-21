package io.teabag.assetbox.email.dto;

import java.util.UUID;

public record SendMessageDto(
        UUID requestId,
        String email,
        String baseUrl,
        String token
) {
}
