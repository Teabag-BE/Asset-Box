package io.teabag.assetbox.message.repository;

import io.teabag.assetbox.message.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
        select m
        from Message m
        where (m.senderId = :myId and m.receiverId = :partnerId)
           or (m.senderId = :partnerId and m.receiverId = :myId)
        order by m.createdAt desc
    """)
    List<Message> findConversation(
            @Param("myId") Long myId,
            @Param("partnerId") Long partnerId
    );

    long countByReceiverIdAndReadFalse(Long receiverId);

    long countBySenderIdAndReceiverIdAndReadFalse(Long senderId, Long receiverId);

    List<Message> findBySenderIdAndReceiverIdAndReadFalse(
            Long senderId,
            Long receiverId
    );

    List<Message> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(Long senderId, Long receiverId);

}
