package io.teabag.assetbox.post.dto;

import java.util.List;

public record PostCreateRequest(String title, String content, Long categoryId, List<String> tags, Long linkedRequestId) {
}
