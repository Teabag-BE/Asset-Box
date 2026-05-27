package io.teabag.assetbox.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email @NotBlank @Size(max = 50) String email,
        @NotBlank @Size(min = 8, max = 50) String password,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(min = 2, max = 30) String nickname,
        @NotBlank @Size(max = 50) String major
) {
}
