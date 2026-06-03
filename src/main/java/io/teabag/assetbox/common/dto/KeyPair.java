package io.teabag.assetbox.common.dto;

public record KeyPair(
        String accessToken,
        String refreshToken
) {
}
