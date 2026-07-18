package io.teabag.assetbox.email.dto;

import io.teabag.assetbox.email.domain.EmailWhiteList;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EnrollEmailRequest(
        @Email String email,
        @NotBlank String name,
        @NotBlank String major
) {
}
