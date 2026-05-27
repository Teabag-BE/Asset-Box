package io.teabag.assetbox.request.dto;

import io.teabag.assetbox.request.domain.RequestStatus;
import java.time.LocalDate;

public record RequestResponse(Long id, String title, String content, RequestStatus status, Long requesterId, Long assigneeId, Long linkedPostId, LocalDate deadline) {
}
