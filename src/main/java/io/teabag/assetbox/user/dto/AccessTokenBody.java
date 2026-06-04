package io.teabag.assetbox.user.dto;

import io.teabag.assetbox.user.constants.Role;
import lombok.Builder;

@Builder
public record AccessTokenBody(
        String email,
        Role role
) {
}
