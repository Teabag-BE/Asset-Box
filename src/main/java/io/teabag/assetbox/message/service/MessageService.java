package io.teabag.assetbox.message.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.message.domain.Message;
import io.teabag.assetbox.message.dto.ConversationSummary;
import io.teabag.assetbox.message.dto.MessageResponse;
import io.teabag.assetbox.message.dto.UnreadCountResponse;
import io.teabag.assetbox.message.repository.MessageRepository;
import io.teabag.assetbox.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public MessageResponse send(Long senderId, Long receiverId, String content) {
        validateUsers(senderId, receiverId);

        Message message = Message.create(senderId, receiverId, content);
        return MessageResponse.from(messageRepository.save(message));
    }

    public List<MessageResponse> getConversation(Long meId, Long partnerId) {
        validateUsers(meId, partnerId);

        return messageRepository.findConversation(meId, partnerId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    public List<ConversationSummary> getInbox(Long meId) {
        PreConditions.validate(userRepository.existsById(meId), ErrorCode.USER_NOT_FOUND);

        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(meId, meId);
        Map<Long, Message> latestMessagesByPartner = new LinkedHashMap<>();

        for (Message message : messages) {
            Long partnerId = getPartnerId(meId, message);
            latestMessagesByPartner.putIfAbsent(partnerId, message);
        }

        List<ConversationSummary> summaries = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : latestMessagesByPartner.entrySet()) {
            Long partnerId = entry.getKey();
            Message lastMessage = entry.getValue();
            long unreadCount = messageRepository.countBySenderIdAndReceiverIdAndReadFalse(partnerId, meId);

            summaries.add(ConversationSummary.of(meId, lastMessage, unreadCount));
        }

        return summaries;
    }

    public UnreadCountResponse getUnreadCount(Long meId) {
        PreConditions.validate(userRepository.existsById(meId), ErrorCode.USER_NOT_FOUND);

        return new UnreadCountResponse(messageRepository.countByReceiverIdAndReadFalse(meId));
    }

    @Transactional
    public void markConversationAsRead(Long meId, Long partnerId) {
        validateUsers(meId, partnerId);

        messageRepository.findBySenderIdAndReceiverIdAndReadFalse(partnerId, meId)
                .forEach(Message::markAsRead);
    }

    private void validateUsers(Long senderId, Long receiverId) {
        PreConditions.validate(!senderId.equals(receiverId), ErrorCode.MESSAGE_SELF_NOT_ALLOWED);
        PreConditions.validate(userRepository.existsById(senderId), ErrorCode.USER_NOT_FOUND);
        PreConditions.validate(userRepository.existsById(receiverId), ErrorCode.USER_NOT_FOUND);
    }

    private Long getPartnerId(Long meId, Message message) {
        if (message.getSenderId().equals(meId)) {
            return message.getReceiverId();
        }
        if (message.getReceiverId().equals(meId)) {
            return message.getSenderId();
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }
}
