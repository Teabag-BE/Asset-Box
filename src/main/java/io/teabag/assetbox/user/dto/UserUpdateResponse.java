package io.teabag.assetbox.user.dto;
import io.teabag.assetbox.user.domain.User;

public record UserUpdateResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String major,
        String description,
        String provider,
        String role,
        String avatarKey
) {
    public static UserUpdateResponse from(User user) {
        return new UserUpdateResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getMajor().name(),
                user.getDecription(),
                user.getProvider(),
                user.getRole().name(),
                user.getAvatarKey()
        );
    }
}
