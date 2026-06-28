package io.teabag.assetbox.message.dto;

import io.teabag.assetbox.message.domain.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        Long receiverId,
        String content,
        boolean read,
        LocalDateTime createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                message.isRead(),
                message.getCreatedAt()
        );
    }
}
