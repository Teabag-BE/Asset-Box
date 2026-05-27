package io.teabag.assetbox.post.dto;

import java.util.List;

public record PostUpdateRequest(String title, String content, Long categoryId, List<String> tags) {
}
