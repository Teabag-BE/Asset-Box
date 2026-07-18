package io.teabag.assetbox.email.dto;

public record SendMessageDto(
        String email,
        String baseUrl,
        String token
) {
}
