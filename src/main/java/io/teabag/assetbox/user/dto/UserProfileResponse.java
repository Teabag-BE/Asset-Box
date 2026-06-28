package io.teabag.assetbox.user.dto;
import io.teabag.assetbox.user.domain.User;

public record UserProfileResponse(
        Long id,
        String email,
        String publicEmail,
        String name,
        String nickname,
        String major,
        String description,
        String provider,
        String role,
        String avatarUrl
) {
    public static UserProfileResponse from(User user, String maskedEmail, String avatarUrl) {
        return new UserProfileResponse(
                user.getId(),
                maskedEmail,
                user.getPublicEmail(),
                user.getName(),
                user.getNickname(),
                user.getMajor().name(),
                user.getDescription(),
                user.getProvider(),
                user.getRole().name(),
                avatarUrl
        );
    }
}
