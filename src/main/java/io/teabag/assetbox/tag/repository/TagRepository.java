package io.teabag.assetbox.tag.repository;

import io.teabag.assetbox.tag.domain.Tag;
import io.teabag.assetbox.tag.dto.PopularTagResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    @Query("""
            select new io.teabag.assetbox.tag.dto.PopularTagResponse(t.name, count(pt))
            from PostTag pt
            join pt.tag t
            group by t.id, t.name
            order by count(pt) desc, t.name asc
            """)
    List<PopularTagResponse> findPopularTags(Pageable pageable);
}
