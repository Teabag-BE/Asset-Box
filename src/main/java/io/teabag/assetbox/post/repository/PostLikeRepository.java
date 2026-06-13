package io.teabag.assetbox.post.repository;

import io.teabag.assetbox.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    @Query("""
        SELECT count(*)
            FROM PostLike pl
            WHERE pl.userId = :userId
    """)
    Integer getCountByUserId(Long userId);
}
