package io.teabag.assetbox.category.controller;

import io.teabag.assetbox.category.dto.CategoryResponse;
import io.teabag.assetbox.category.dto.CategoryTreeResponse;
import io.teabag.assetbox.category.service.CategoryService;
import io.teabag.assetbox.common.dto.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> findAll() {
        return ApiResponse.ok(categoryService.findAll(), "Category list found");
    }

    @GetMapping("/tree")
    public ApiResponse<List<CategoryTreeResponse>> findTree() {
        return ApiResponse.ok(categoryService.findTree(), "Category tree found");
    }

    @GetMapping("/roots")
    public ApiResponse<List<CategoryResponse>> roots() {
        return ApiResponse.ok(categoryService.roots(), "Root categories found");
    }

    @GetMapping("/{parentId}/children")
    public ApiResponse<List<CategoryResponse>> children(@PathVariable Long parentId) {
        return ApiResponse.ok(categoryService.children(parentId), "Child categories found");
    }
}