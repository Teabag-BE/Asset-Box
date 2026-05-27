package io.teabag.assetbox.post.repository;

import io.teabag.assetbox.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
}
