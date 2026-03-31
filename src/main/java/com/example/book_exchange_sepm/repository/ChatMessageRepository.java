package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all messages in a chat room ordered by creation time (oldest first)
     */
    List<ChatMessage> findByChatRoom_IdOrderByCreatedAtAsc(Long chatRoomId);

    /**
     * Find all messages in a chat room ordered by creation time (newest first)
     */
    List<ChatMessage> findByChatRoom_IdOrderByCreatedAtDesc(Long chatRoomId);

    /**
     * Find all messages in a chat room for a specific sender
     */
    @Query("""
        select m from ChatMessage m
        where m.chatRoom.id = :chatRoomId and m.sender.id = :userId
        order by m.createdAt asc
        """)
    List<ChatMessage> findByChatRoomAndSender(@Param("chatRoomId") Long chatRoomId,
                                              @Param("userId") Long userId);
}

