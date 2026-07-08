package io.teabag.assetbox.ai.dto;

import java.util.List;

// AI 추천 요청 (제목 / 파일명 목록 / 썸네일 base64)
// thumbnailBase64는 선택값이며 data URL 또는 순수 base64 문자열 모두 허용한다.
public record AiSuggestRequest(
        String title,
        List<String> filenames,
        String thumbnailBase64
) {
}
