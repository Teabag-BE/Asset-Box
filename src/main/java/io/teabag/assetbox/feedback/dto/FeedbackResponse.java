package io.teabag.assetbox.feedback.dto;

import io.teabag.assetbox.feedback.domain.FeedbackStatus;

public record FeedbackResponse(Long id, String title, String content, Long userId, String userNickname, FeedbackStatus status) {
}
