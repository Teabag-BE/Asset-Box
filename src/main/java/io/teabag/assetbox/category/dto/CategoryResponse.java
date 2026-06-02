package io.teabag.assetbox.category.dto;

import io.teabag.assetbox.category.domain.Category;

public record CategoryResponse(
        Long id,
        String name,
        Long parentId,
        int depth
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getDepth()
        );
    }
}