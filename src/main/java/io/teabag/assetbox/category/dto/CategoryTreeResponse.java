package io.teabag.assetbox.category.dto;

import java.util.List;

public record CategoryTreeResponse(
        Long id,
        String name,
        int depth,
        List<CategoryTreeResponse> children
) {
}