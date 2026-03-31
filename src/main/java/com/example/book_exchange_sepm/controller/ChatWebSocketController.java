package com.example.book_exchange_sepm.controller;

import com.example.book_exchange_sepm.dto.ChatMessageRequest;
import com.example.book_exchange_sepm.dto.ChatMessageResponse;
import com.example.book_exchange_sepm.service.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Send message to an exchange chat room
     * Route: /app/exchange/{exchangeRequestId}/send
     * Messages are broadcast to /topic/chat/{exchangeRequestId}
     */
    @MessageMapping("/exchange/{exchangeRequestId}/send")
    public void sendMessageToExchange(
            @DestinationVariable Long exchangeRequestId,
            @Payload ChatMessageRequest request,
            Principal principal) {
        sendMessage(exchangeRequestId, request, principal);
    }

    @MessageMapping("/chat/{exchangeRequestId}/send")
    public void sendMessageToChatAlias(
            @DestinationVariable Long exchangeRequestId,
            @Payload ChatMessageRequest request,
            Principal principal) {
        sendMessage(exchangeRequestId, request, principal);
    }

    private void sendMessage(Long exchangeRequestId,
                             ChatMessageRequest request,
                             Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return;
        }

        // Validate user access and send message
        ChatMessageResponse response = chatRoomService.sendMessageToRoomAsUser(
            exchangeRequestId,
            request.getContent(),
            principal.getName()
        );

        // Broadcast message to all subscribers of this exchange's topic
        messagingTemplate.convertAndSend(
            "/topic/chat/" + exchangeRequestId,
            response
        );

        // Backward-compatible topic for existing clients.
        messagingTemplate.convertAndSend(
            "/topic/exchange/" + exchangeRequestId,
            response
        );
    }
}
