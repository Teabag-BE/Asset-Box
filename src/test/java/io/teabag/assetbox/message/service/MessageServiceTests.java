package io.teabag.assetbox.message.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.message.domain.Message;
import io.teabag.assetbox.message.dto.MessageResponse;
import io.teabag.assetbox.message.repository.MessageRepository;
import io.teabag.assetbox.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.never;

@ExtendWith(MockitoExtension.class)
class MessageServiceTests {

    @Mock
    MessageRepository messageRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    MessageService messageService;

    @Test
    @DisplayName("메시지를 저장하고 응답으로 변환한다")
    void sendMessage() {
        // given
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRepository.existsById(2L)).willReturn(true);
        given(messageRepository.save(any(Message.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        MessageResponse response = messageService.send(1L, 2L, "안녕하세요");

        // then
        assertThat(response.senderId()).isEqualTo(1L);
        assertThat(response.receiverId()).isEqualTo(2L);
        assertThat(response.content()).isEqualTo("안녕하세요");
        assertThat(response.read()).isFalse();

        then(messageRepository).should().save(any(Message.class));
    }

    @Test
    @DisplayName("자기 자신에게 메시지를 보낼 수 없다")
    void sendMessageToSelf() {
        // when & then
        assertThatThrownBy(() -> messageService.send(1L, 1L, "셀프 메시지"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MESSAGE_SELF_NOT_ALLOWED);

        then(messageRepository).should(never()).save(any(Message.class));
    }
}
