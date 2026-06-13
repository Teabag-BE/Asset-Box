package io.teabag.assetbox.user.dto;

import io.teabag.assetbox.user.constants.Role;
import jakarta.validation.constraints.NotNull;

public record UserUpdateRoleRequest(
       @NotNull Role role
) {
}
