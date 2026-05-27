package io.teabag.assetbox.message.dto;

public record ConversationSummary(Long userId, String nickname, String lastMessage, long unreadCount) {
}
