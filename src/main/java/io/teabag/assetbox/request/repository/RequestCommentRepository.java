package io.teabag.assetbox.request.repository;

import io.teabag.assetbox.request.domain.RequestComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestCommentRepository extends JpaRepository<RequestComment, Long> {
}
