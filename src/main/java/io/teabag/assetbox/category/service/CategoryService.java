package io.teabag.assetbox.category.service;

import io.teabag.assetbox.category.domain.Category;
import io.teabag.assetbox.category.dto.CategoryResponse;
import io.teabag.assetbox.category.dto.CategoryTreeResponse;
import io.teabag.assetbox.category.repository.CategoryRepository;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryTreeResponse> findTree() {
        List<Category> allCategories = categoryRepository.findAll();

        if (allCategories.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        return allCategories.stream()
                .filter(c -> c.getParentId() == null)
                .map(root -> buildTree(root, childrenMap))
                .toList();
    }

    private CategoryTreeResponse buildTree(Category category,
                                           Map<Long, List<Category>> childrenMap) {
        List<Category> children = childrenMap.getOrDefault(category.getId(), List.of());
        List<CategoryTreeResponse> childResponses = children.stream()
                .map(child -> buildTree(child, childrenMap))
                .toList();

        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getDepth(),
                childResponses
        );
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .toList();
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
        return categoryRepository.findByIdOrThrow(categoryId);
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