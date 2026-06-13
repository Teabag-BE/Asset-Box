package io.teabag.assetbox.user.dto;

import io.teabag.assetbox.user.constants.Role;
import lombok.Builder;

@Builder
public record TokenBody(
        String email
) {
}
