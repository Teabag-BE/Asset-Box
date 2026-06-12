package io.teabag.assetbox.message.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.util.PreConditions;
import io.teabag.assetbox.message.domain.Message;
import io.teabag.assetbox.message.dto.MessageResponse;
import io.teabag.assetbox.message.repository.MessageRepository;
import io.teabag.assetbox.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private void validateUsers(Long senderId, Long receiverId) {
        PreConditions.validate(!senderId.equals(receiverId), ErrorCode.MESSAGE_SELF_NOT_ALLOWED);
        PreConditions.validate(userRepository.existsById(senderId), ErrorCode.USER_NOT_FOUND);
        PreConditions.validate(userRepository.existsById(receiverId), ErrorCode.USER_NOT_FOUND);
    }
}
