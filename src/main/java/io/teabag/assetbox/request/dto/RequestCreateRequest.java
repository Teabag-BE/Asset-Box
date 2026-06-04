package io.teabag.assetbox.request.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record RequestCreateRequest(
        @NotBlank
        @Size(max=100)
        String title,

        @NotBlank
        String content,

        @Size(max=60)
        String assetType,

        @Size(max=60)
        String preferredStyle,

        @Size(max=60)
        String engine,

        @FutureOrPresent
        LocalDateTime deadline,

        // TODO: 인증 붙으면 request body에서 받지 말고 SecurityContext에서 꺼내기
        @NotNull
        Long requesterId

) {
}
