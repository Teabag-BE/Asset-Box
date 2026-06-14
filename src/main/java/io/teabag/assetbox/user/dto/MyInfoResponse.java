package io.teabag.assetbox.user.dto;
import io.teabag.assetbox.user.domain.User;

public record MyInfoResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String major,
        String description,
        String provider,
        String role,
        String avatarUrl
) {
    public static MyInfoResponse from(User user,String avatarUrl) {
        return new MyInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getMajor().name(),
                user.getDecription(),
                user.getProvider(),
                user.getRole().name(),
                avatarUrl
        );
    }
}
