package io.teabag.assetbox.ai.controller;

import io.teabag.assetbox.ai.dto.AiStatusResponse;
import io.teabag.assetbox.ai.dto.AiSuggestRequest;
import io.teabag.assetbox.ai.dto.AiSuggestResponse;
import io.teabag.assetbox.ai.service.AiSuggestService;
import io.teabag.assetbox.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {

    private final AiSuggestService aiSuggestService;

    // AI 추천 기능 활성화 여부 조회
    @GetMapping("/status")
    public ApiResponse<AiStatusResponse> status() {
        AiStatusResponse response = aiSuggestService.status();
        return ApiResponse.ok(response, "AI 추천 기능 상태를 조회했습니다.");
    }

    // 제목 / 파일명 / 썸네일 기반 태그·카테고리 추천
    @PostMapping("/suggest")
    public ApiResponse<AiSuggestResponse> suggest(@RequestBody AiSuggestRequest request) {
        AiSuggestResponse response = aiSuggestService.suggest(request);
        return ApiResponse.ok(response, "AI 추천을 완료했습니다.");
    }
}
