package io.teabag.assetbox.message.service;

import io.teabag.assetbox.common.constants.ErrorCode;
import io.teabag.assetbox.common.exception.BusinessException;
import io.teabag.assetbox.message.domain.Message;
import io.teabag.assetbox.message.dto.ConversationSummary;
import io.teabag.assetbox.message.dto.MessageResponse;
import io.teabag.assetbox.message.repository.MessageRepository;
import io.teabag.assetbox.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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

    @Test
    @DisplayName("인박스는 상대별 최신 메시지와 unread count를 반환한다")
    void getInbox() {
        // given
        given(userRepository.existsById(1L)).willReturn(true);

        Message latestWithUser2 = Message.create(2L, 1L, "최근 메시지");
        Message oldWithUser2 = Message.create(1L, 2L, "이전 메시지");
        Message latestWithUser3 = Message.create(1L, 3L, "다른 상대 메시지");

        given(messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(1L, 1L))
                .willReturn(List.of(latestWithUser2, oldWithUser2, latestWithUser3));
        given(messageRepository.countBySenderIdAndReceiverIdAndReadFalse(2L, 1L))
                .willReturn(2L);
        given(messageRepository.countBySenderIdAndReceiverIdAndReadFalse(3L, 1L))
                .willReturn(0L);

        // when
        List<ConversationSummary> summaries = messageService.getInbox(1L);

        // then
        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).partnerId()).isEqualTo(2L);
        assertThat(summaries.get(0).lastMessage()).isEqualTo("최근 메시지");
        assertThat(summaries.get(0).unreadCount()).isEqualTo(2L);
        assertThat(summaries.get(1).partnerId()).isEqualTo(3L);
        assertThat(summaries.get(1).unreadCount()).isZero();
    }
}
