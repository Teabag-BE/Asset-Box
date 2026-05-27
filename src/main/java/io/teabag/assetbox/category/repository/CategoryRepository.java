package io.teabag.assetbox.category.repository;

import io.teabag.assetbox.category.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIdOrderBySortOrderAsc(Long parentId);
    List<Category> findByDepthOrderBySortOrderAsc(int depth);
    boolean existsByNameAndParentId(String name, Long parentId);
}
