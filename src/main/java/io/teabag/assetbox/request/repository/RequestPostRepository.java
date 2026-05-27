package io.teabag.assetbox.request.repository;

import io.teabag.assetbox.request.domain.RequestPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestPostRepository extends JpaRepository<RequestPost, Long> {
}
