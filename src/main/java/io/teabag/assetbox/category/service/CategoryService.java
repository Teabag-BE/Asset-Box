package io.teabag.assetbox.category.service;

import io.teabag.assetbox.category.domain.Category;
import io.teabag.assetbox.category.dto.CategoryResponse;
import io.teabag.assetbox.category.repository.CategoryRepository;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> roots() {
        return categoryRepository.findByDepthOrderByIdAsc(1)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public List<CategoryResponse> children(Long parentId) {
        requireExists(parentId);

        return categoryRepository.findByParentIdOrderByIdAsc(parentId)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public Category requireExists(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found"
                ));
    }

    public Category requireLeaf(Long categoryId) {
        Category category = requireExists(categoryId);

        if (category.getDepth() != 3) {
            throw new BusinessException(
                    ErrorCode.CATEGORY_DEPTH_INVALID,
                    "Post category must be depth 3"
            );
        }

        return category;
    }
}