package io.teabag.assetbox.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 2, max = 30) String nickname,
        @Size(max = 50) String major,
        @Email @Size(max = 50) String publicEmail,
        String description
) {

}
