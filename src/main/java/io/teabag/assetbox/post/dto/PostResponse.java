package io.teabag.assetbox.post.dto;

import java.util.List;

public record PostResponse(Long id, String title, String content, Long authorId, Long categoryId, List<String> categoryPath, Long thumbnailFileId, String thumbnailUrl, List<String> tags, Long linkedRequestId) {
}
