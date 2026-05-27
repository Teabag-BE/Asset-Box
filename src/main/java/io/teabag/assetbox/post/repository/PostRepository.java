package io.teabag.assetbox.post.repository;

import io.teabag.assetbox.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
