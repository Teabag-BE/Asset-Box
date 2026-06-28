package io.teabag.assetbox.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email @NotBlank @Size(max = 50) String email,
        @NotBlank @Size(min = 8, max = 50, message = "비밀번호 길이는 8 ~ 50자 사이이어야합니다.") String password,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(min = 2, max = 30) String nickname,
        @NotBlank @Size(max = 50) String major
) {
}
