package io.teabag.assetbox.message.dto;

import io.teabag.assetbox.message.domain.Message;

import java.time.LocalDateTime;

public record ConversationSummary(
        Long partnerId,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
    public static ConversationSummary of(Long meId, Message lastMessage, long unreadCount) {
        Long partnerId = lastMessage.getSenderId().equals(meId)
                ? lastMessage.getReceiverId()
                : lastMessage.getSenderId();

        return new ConversationSummary(
                partnerId,
                lastMessage.getContent(),
                lastMessage.getCreatedAt(),
                unreadCount
        );
    }
}
