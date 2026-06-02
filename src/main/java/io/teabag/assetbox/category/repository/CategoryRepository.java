package io.teabag.assetbox.category.repository;

import io.teabag.assetbox.category.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIdOrderByIdAsc(Long parentId);

    List<Category> findByDepthOrderByIdAsc(int depth);

    boolean existsByNameAndParentId(String name, Long parentId);
}