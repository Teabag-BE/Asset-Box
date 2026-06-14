package io.teabag.assetbox.request.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
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

        // TODO: 기존 호출부 호환용 필드. 인증 사용 시 SecurityContext의 사용자 id를 우선 사용한다.
        Long requesterId

) {
}
