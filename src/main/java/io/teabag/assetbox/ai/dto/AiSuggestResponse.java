package io.teabag.assetbox.ai.dto;

import java.util.List;

// AI 추천 결과 (태그 목록 / 선택된 카테고리 id / 카테고리 경로 문자열)
public record AiSuggestResponse(
        List<String> tags,
        Long categoryId,
        String categoryPath
) {
}
