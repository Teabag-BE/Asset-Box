package io.teabag.assetbox.category.repository;

import io.teabag.assetbox.category.domain.Category;
import java.util.List;
import java.util.Optional;

import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.constants.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIdOrderByIdAsc(Long parentId);

    List<Category> findByDepthOrderByIdAsc(int depth);

    Optional<Category> findById(Long categoryId);

    default Category findByIdOrThrow(Long category){
        return findById(category).orElseThrow(
                ()-> new BusinessException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found"
                )
        );
    }

    boolean existsByNameAndParentId(String name, Long parentId);
}