package io.teabag.assetbox.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teabag.assetbox.ai.dto.AiStatusResponse;
import io.teabag.assetbox.ai.dto.AiSuggestRequest;
import io.teabag.assetbox.ai.dto.AiSuggestResponse;
import io.teabag.assetbox.category.dto.CategoryResponse;
import io.teabag.assetbox.category.service.CategoryService;
import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class AiSuggestService {

    private static final String OPENAI_BASE_URL = "https://api.openai.com/v1";

    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiSuggestService(
            CategoryService categoryService,
            ObjectMapper objectMapper,
            @Value("${custom.openai.api-key:}") String apiKey,
            @Value("${custom.openai.model:gpt-4o-mini}") String model
    ) {
        this.categoryService = categoryService;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;

        // OpenAI 호출용 RestClient. 연결 5초 / 응답 30초 타임아웃.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(OPENAI_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    // API 키 설정 여부 반환 (공백이면 비활성화)
    public AiStatusResponse status() {
        return new AiStatusResponse(isEnabled());
    }

    private boolean isEnabled() {
        return StringUtils.hasText(apiKey);
    }

    // 제목 / 파일명 / 썸네일을 바탕으로 태그와 카테고리를 추천한다.
    public AiSuggestResponse suggest(AiSuggestRequest request) {
        if (!isEnabled()) {
            throw new BusinessException(ErrorCode.AI_DISABLED);
        }

        // 카테고리 정본은 DB. 모델에게 목록을 넘겨주고, 반환된 id도 이 목록으로만 검증한다.
        List<CategoryResponse> categories = categoryService.findAll();

        String content = callOpenAi(request, categories);

        // 모델이 돌려준 JSON 문자열을 파싱. 실패 시 태그 비움 / 카테고리 null로 완만하게 처리.
        List<String> tags = new ArrayList<>();
        Long categoryId = null;
        try {
            JsonNode parsed = objectMapper.readTree(content);
            JsonNode tagsNode = parsed.get("tags");
            if (tagsNode != null && tagsNode.isArray()) {
                for (JsonNode tagNode : tagsNode) {
                    String tag = tagNode.asText("").trim();
                    // 앞에 붙은 '#' 은 제거한다.
                    if (tag.startsWith("#")) {
                        tag = tag.substring(1).trim();
                    }
                    if (StringUtils.hasText(tag)) {
                        tags.add(tag);
                    }
                }
            }
            JsonNode categoryIdNode = parsed.get("categoryId");
            if (categoryIdNode != null && categoryIdNode.canConvertToLong()) {
                categoryId = categoryIdNode.asLong();
            }
        } catch (Exception e) {
            // 파싱 실패는 500이 아닌 빈 추천으로 처리한다.
            return new AiSuggestResponse(List.of(), null, null);
        }

        // 반환된 categoryId가 실제 존재하는지 검증. 없으면 null.
        Map<Long, CategoryResponse> byId = new HashMap<>();
        for (CategoryResponse category : categories) {
            byId.put(category.id(), category);
        }
        if (categoryId == null || !byId.containsKey(categoryId)) {
            return new AiSuggestResponse(tags, null, null);
        }

        String categoryPath = buildCategoryPath(categoryId, byId);
        return new AiSuggestResponse(tags, categoryId, categoryPath);
    }

    // parentId를 타고 올라가 루트 → 리프 순서로 이름을 " > " 로 연결한다.
    private String buildCategoryPath(Long categoryId, Map<Long, CategoryResponse> byId) {
        List<String> names = new ArrayList<>();
        Long cursor = categoryId;
        // 순환 방지를 위해 최대 깊이를 제한한다.
        int guard = 0;
        while (cursor != null && guard < 10) {
            CategoryResponse category = byId.get(cursor);
            if (category == null) {
                break;
            }
            names.add(0, category.name());
            cursor = category.parentId();
            guard++;
        }
        return String.join(" > ", names);
    }

    // OpenAI Chat Completions 호출. 실패 시 AI_REQUEST_FAILED 로 던진다.
    private String callOpenAi(AiSuggestRequest request, List<CategoryResponse> categories) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userContent(request, categories))
        ));

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.AI_REQUEST_FAILED);
            }
            JsonNode message = response.path("choices").path(0).path("message").path("content");
            if (message.isMissingNode() || !message.isTextual()) {
                throw new BusinessException(ErrorCode.AI_REQUEST_FAILED);
            }
            return message.asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // OpenAI 통신 실패(비2xx / 타임아웃 등)는 하드 실패로 처리한다.
            throw new BusinessException(ErrorCode.AI_REQUEST_FAILED, e.getMessage());
        }
    }

    private String systemPrompt() {
        return """
                당신은 3D 에셋에 태그와 카테고리를 붙이는 도우미입니다.
                반드시 다음 형태의 JSON만 응답하세요: {"tags":[한국어 태그...],"categoryId":<id 또는 null>}.
                - tags: 3~6개의 짧은 한국어 태그. 앞에 '#'을 붙이지 마세요.
                - categoryId: 제공된 카테고리 목록에서만 고르며, 가장 구체적으로 일치하는 하나를 선택합니다.
                  일치하는 것이 없으면 null 로 둡니다. 목록에 없는 id를 절대 만들어내지 마세요.
                """;
    }

    // 사용자 메시지 구성. 썸네일이 있으면 image_url 파트를 함께 넣는다.
    private Object userContent(AiSuggestRequest request, List<CategoryResponse> categories) {
        String text = buildUserText(request, categories);

        String imageUrl = resolveImageUrl(request.thumbnailBase64());
        if (imageUrl == null) {
            // 썸네일이 없으면 단순 텍스트로 보낸다.
            return text;
        }

        // 썸네일이 있으면 텍스트 + 이미지 멀티파트로 보낸다.
        return List.of(
                Map.of("type", "text", "text", text),
                Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
        );
    }

    private String buildUserText(AiSuggestRequest request, List<CategoryResponse> categories) {
        String title = request.title() == null ? "" : request.title();
        String filenames = request.filenames() == null ? "" : String.join(", ", request.filenames());

        StringBuilder categoryList = new StringBuilder();
        for (CategoryResponse category : categories) {
            categoryList.append(category.id())
                    .append(':').append(category.name())
                    .append(" (parentId=").append(category.parentId())
                    .append(", depth=").append(category.depth())
                    .append(")\n");
        }

        return "제목: " + title + "\n"
                + "파일: " + filenames + "\n"
                + "선택 가능한 카테고리(id:name, parentId, depth):\n" + categoryList;
    }

    // data URL이면 그대로, 순수 base64면 png data URL 접두사를 붙인다. 없으면 null.
    private String resolveImageUrl(String thumbnailBase64) {
        if (!StringUtils.hasText(thumbnailBase64)) {
            return null;
        }
        String trimmed = thumbnailBase64.trim();
        if (trimmed.startsWith("data:")) {
            return trimmed;
        }
        return "data:image/png;base64," + trimmed;
    }
}
