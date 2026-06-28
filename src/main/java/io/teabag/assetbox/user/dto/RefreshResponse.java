package io.teabag.assetbox.user.dto;

import lombok.Builder;

@Builder
public record RefreshResponse(
        String accessToken,
        String tokenType
) {
}
