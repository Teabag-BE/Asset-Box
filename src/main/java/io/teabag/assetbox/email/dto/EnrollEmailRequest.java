package io.teabag.assetbox.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.teabag.assetbox.email.domain.EmailWhiteList;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EnrollEmailRequest(
        @Schema(
                description = "이메일",
                example = "wjdtn747@naver.com"
        )
        @Email String email,
        @Schema(
                description = "사용자 이름",
                example = "이정수"
        )
        @NotBlank String name,
        @Schema(
                description = "전공",
                example = "BACK_END"
        )
        @NotBlank String major
) {
}
