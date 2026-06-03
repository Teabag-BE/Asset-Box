package io.teabag.assetbox.user.dto;


import io.teabag.assetbox.common.constants.TokenType;
import lombok.Builder;

@Builder
public record LoginResponse(
        String accessToken,
        TokenType tokenType
) {
}
