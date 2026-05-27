package io.teabag.assetbox.request.dto;

import io.teabag.assetbox.request.domain.RequestStatus;

public record RequestStatusChangeRequest(RequestStatus status) {
}
