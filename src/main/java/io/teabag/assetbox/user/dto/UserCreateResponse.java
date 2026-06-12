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
        String role,
        String avatarUrl
) {
    public static UserCreateResponse from(User user) {
        String avatarUrl = user.getAvatarPath() == null ? null : "/api/users/" + user.getId() + "/avatar";
        return new UserCreateResponse(user.getId(), user.getEmail(), user.getName(), user.getNickname(), user.getMajor().name(), user.getProvider(), user.getRole().name(), avatarUrl);
    }
}
