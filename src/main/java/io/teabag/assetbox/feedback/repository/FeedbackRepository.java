package io.teabag.assetbox.feedback.repository;

import io.teabag.assetbox.feedback.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
