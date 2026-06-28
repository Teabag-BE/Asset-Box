package io.teabag.assetbox.user.dto;

import io.teabag.assetbox.user.constants.Role;
import io.teabag.assetbox.user.domain.User;

public record UserCreateResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String major,
        String provider,
        String role
) {
    public static UserCreateResponse from(User user) {
        return new UserCreateResponse(user.getId(), user.getEmail(), user.getName(), user.getNickname(), user.getMajor().name(), user.getProvider(), user.getRole().name());
    }
}
