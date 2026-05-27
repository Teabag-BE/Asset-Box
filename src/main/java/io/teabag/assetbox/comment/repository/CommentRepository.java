package io.teabag.assetbox.comment.repository;

import io.teabag.assetbox.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
