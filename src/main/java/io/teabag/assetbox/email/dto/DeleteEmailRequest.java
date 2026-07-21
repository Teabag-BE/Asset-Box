package io.teabag.assetbox.email.dto;

import jakarta.validation.constraints.Email;

public record DeleteEmailRequest(
        @Email String email
) {
}
