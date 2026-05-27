package io.teabag.assetbox.message.dto;

public record MessageSendRequest(Long receiverId, String content) {
}
