package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.ChatMessageRequest;
import com.example.book_exchange_sepm.dto.ChatMessageResponse;
import com.example.book_exchange_sepm.dto.ExchangeChatRoomResponse;
import com.example.book_exchange_sepm.service.ChatRoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exchange")
@PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
public class ChatController {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Get messages for an exchange's chat room
     * Only accessible to users involved in the exchange
     */
    @GetMapping("/{exchangeRequestId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getExchangeMessages(@PathVariable Long exchangeRequestId) {
        List<ChatMessageResponse> messages = chatRoomService.getChatRoomMessages(exchangeRequestId);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    /**
     * Send a message to an exchange's chat room via HTTP
     * Fallback when WebSocket is unavailable
     */
    @PostMapping("/{exchangeRequestId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessageToExchange(
            @PathVariable Long exchangeRequestId,
            @Valid @RequestBody ChatMessageRequest request) {
        ChatMessageResponse response = chatRoomService.sendMessageToRoom(exchangeRequestId, request.getContent());

        messagingTemplate.convertAndSend(
            "/topic/chat/" + exchangeRequestId,
            response
        );

        // Backward-compatible topic for existing clients.
        messagingTemplate.convertAndSend(
            "/topic/exchange/" + exchangeRequestId,
            response
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all active chat rooms for the current user's exchanges
     * Lists all exchanges with a last message preview
     */
    @GetMapping("/my-chats")
    public ResponseEntity<List<ExchangeChatRoomResponse>> getActiveChatRooms() {
        List<ExchangeChatRoomResponse> chats = chatRoomService.getActiveChatsForCurrentUser();
        return new ResponseEntity<>(chats, HttpStatus.OK);
    }

    /**
     * Get specific exchange chat room details
     */
    @GetMapping("/{exchangeRequestId}/chat")
    public ResponseEntity<ExchangeChatRoomResponse> getExchangeChatRoom(@PathVariable Long exchangeRequestId) {
        List<ExchangeChatRoomResponse> chats = chatRoomService.getActiveChatsForCurrentUser();
        ExchangeChatRoomResponse room = chats.stream()
            .filter(r -> r.getExchangeRequestId().equals(exchangeRequestId))
            .findFirst()
            .orElse(null);
        
        if (room == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(room, HttpStatus.OK);
    }
}

