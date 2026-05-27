package io.teabag.assetbox.user.dto;

public record LoginResponse(String accessToken, String tokenType, boolean profileRequired) {
}
