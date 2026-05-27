package io.teabag.assetbox.message.dto;

public record MessageResponse(Long id, Long senderId, Long receiverId, String content, boolean read) {
}
