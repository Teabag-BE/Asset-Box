package io.teabag.assetbox.request.dto;

import java.time.LocalDate;

public record RequestCreateRequest(String title, String content, String assetType, String preferredStyle, String engine, LocalDate deadline) {
}
