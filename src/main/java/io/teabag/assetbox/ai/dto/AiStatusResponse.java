package io.teabag.assetbox.ai.dto;

// AI 추천 기능 활성화 여부 응답 (Gemini API 키 설정 여부)
public record AiStatusResponse(
        boolean enabled
) {
}
