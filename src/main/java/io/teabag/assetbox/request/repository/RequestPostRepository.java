package io.teabag.assetbox.request.repository;

import io.teabag.assetbox.request.domain.RequestPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RequestPostRepository extends JpaRepository<RequestPost, Long> {
    @Query("""
        SELECT count(*)
            FROM RequestPost rp
            WHERE rp.requesterId = :userId
    """)
    Integer getCountByRequesterId(Long userId);
}
