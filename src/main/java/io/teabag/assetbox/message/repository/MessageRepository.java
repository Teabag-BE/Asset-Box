package io.teabag.assetbox.message.repository;

import io.teabag.assetbox.message.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
