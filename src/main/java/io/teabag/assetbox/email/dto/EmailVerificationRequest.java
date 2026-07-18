package io.teabag.assetbox.email.dto;

import jakarta.validation.constraints.Email;

public record EmailVerificationRequest(
        @Email String email
) {
}
