package io.teabag.assetbox.post.dto;

import java.util.List;

public record PostSummaryResponse(
        Long id,
        String title,
        Long authorId,
        Long categoryId,
        List<String> categoryPath,
        Long thumbnailFileId,
        String thumbnailUrl,
        List<String> tags,
        Long linkedRequestId) {
}
