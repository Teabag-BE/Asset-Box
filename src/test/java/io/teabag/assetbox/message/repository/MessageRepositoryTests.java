package io.teabag.assetbox.message.repository;

import io.teabag.assetbox.message.domain.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
class MessageRepositoryTests {

    @Autowired
    MessageRepository messageRepository;

    @Nested
    @DisplayName("메시지 저장")
    class 메시지_저장 {

        @Test
        @DisplayName("Message 엔티티를 저장할 수 있다")
        void saveMessage() {
            // given
            Message message = Message.create(1L, 2L, "안녕하세요");

            // when
            Message savedMessage = messageRepository.saveAndFlush(message);

            // then
            assertThat(savedMessage.getId()).isNotNull();
            assertThat(savedMessage.getSenderId()).isEqualTo(1L);
            assertThat(savedMessage.getReceiverId()).isEqualTo(2L);
            assertThat(savedMessage.getContent()).isEqualTo("안녕하세요");
            assertThat(savedMessage.isRead()).isFalse();
            assertThat(savedMessage.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("메시지 도메인 기능")
    class 메시지_도메인_기능 {

        @Test
        @DisplayName("정적 팩토리 생성 시 읽지 않은 상태로 생성된다")
        void createMessage() {
            // when
            Message message = Message.create(1L, 2L, "메시지");

            // then
            assertThat(message.getSenderId()).isEqualTo(1L);
            assertThat(message.getReceiverId()).isEqualTo(2L);
            assertThat(message.getContent()).isEqualTo("메시지");
            assertThat(message.isRead()).isFalse();
            assertThat(message.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("메시지를 읽음 처리할 수 있다")
        void markAsRead() {
            // given
            Message message = Message.create(1L, 2L, "메시지");

            // when
            message.markAsRead();

            // then
            assertThat(message.isRead()).isTrue();
        }
    }

    @Nested
    @DisplayName("메시지 조회")
    class 메시지_조회 {

        @Test
        @DisplayName("특정 두 사용자 간 메시지를 최신순으로 조회할 수 있다")
        void findConversationOrderByCreatedAtDesc() throws InterruptedException {
            // given
            Message firstMessage = messageRepository.save(Message.create(1L, 2L, "첫 번째 메시지"));
            Thread.sleep(5);
            Message secondMessage = messageRepository.save(Message.create(2L, 1L, "두 번째 메시지"));
            messageRepository.save(Message.create(1L, 3L, "다른 사용자 메시지"));
            messageRepository.flush();

            // when
            List<Message> messages = messageRepository.findConversation(1L, 2L);

            // then
            assertThat(messages).hasSize(2);
            assertThat(messages)
                    .extracting(Message::getId)
                    .containsExactly(secondMessage.getId(), firstMessage.getId());
        }

        @Test
        @DisplayName("특정 사용자의 안 읽은 메시지 개수를 조회할 수 있다")
        void countUnreadMessagesByReceiverId() {
            // given
            messageRepository.save(Message.create(1L, 2L, "읽지 않은 메시지 1"));
            messageRepository.save(Message.create(3L, 2L, "읽지 않은 메시지 2"));

            Message readMessage = Message.create(4L, 2L, "읽은 메시지");
            readMessage.markAsRead();
            messageRepository.save(readMessage);

            messageRepository.save(Message.create(1L, 3L, "다른 수신자 메시지"));
            messageRepository.flush();

            // when
            long unreadCount = messageRepository.countByReceiverIdAndReadFalse(2L);

            // then
            assertThat(unreadCount).isEqualTo(2L);
        }

        @Test
        @DisplayName("특정 상대가 보낸 안 읽은 메시지를 조회할 수 있다")
        void findUnreadMessagesBySenderIdAndReceiverId() {
            // given
            Message unreadMessage = messageRepository.save(Message.create(1L, 2L, "읽지 않은 메시지"));

            Message readMessage = Message.create(1L, 2L, "읽은 메시지");
            readMessage.markAsRead();
            messageRepository.save(readMessage);

            messageRepository.save(Message.create(3L, 2L, "다른 발신자 메시지"));
            messageRepository.save(Message.create(1L, 3L, "다른 수신자 메시지"));
            messageRepository.flush();

            // when
            List<Message> messages = messageRepository.findBySenderIdAndReceiverIdAndReadFalse(1L, 2L);

            // then
            assertThat(messages).hasSize(1);
            assertThat(messages.getFirst().getId()).isEqualTo(unreadMessage.getId());
        }
    }

}
