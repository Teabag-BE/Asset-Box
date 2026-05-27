package io.teabag.assetbox.user.dto;

import io.teabag.assetbox.user.domain.AuthProvider;
import io.teabag.assetbox.user.domain.Role;
import io.teabag.assetbox.user.domain.User;

public record UserResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String major,
        AuthProvider provider,
        Role role,
        String avatarUrl
) {
    public static UserResponse from(User user) {
        String avatarUrl = user.getAvatarPath() == null ? null : "/api/users/" + user.getId() + "/avatar";
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getNickname(), user.getMajor(), user.getProvider(), user.getRole(), avatarUrl);
    }
}
