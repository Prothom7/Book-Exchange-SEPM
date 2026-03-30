package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.dto.ChatMessageResponse;
import com.example.book_exchange_sepm.dto.ExchangeChatRoomResponse;
import com.example.book_exchange_sepm.entity.ChatMessage;
import com.example.book_exchange_sepm.entity.ChatRoom;
import com.example.book_exchange_sepm.entity.ExchangeRequest;
import com.example.book_exchange_sepm.entity.User;
import com.example.book_exchange_sepm.exception.ResourceNotFoundException;
import com.example.book_exchange_sepm.exception.UnauthorizedActionException;
import com.example.book_exchange_sepm.repository.ChatMessageRepository;
import com.example.book_exchange_sepm.repository.ChatRoomRepository;
import com.example.book_exchange_sepm.repository.ExchangeRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private UserService userService;

    /**
     * Create a chat room for an exchange request.
     * Automatically called when exchange request is created.
     */
    @Transactional
    public ChatRoom createChatRoomForExchange(ExchangeRequest exchangeRequest) {
        return chatRoomRepository.findByExchangeRequest_Id(exchangeRequest.getId())
            .orElseGet(() -> {
                ChatRoom chatRoom = new ChatRoom();
                chatRoom.setExchangeRequest(exchangeRequest);
                return chatRoomRepository.save(chatRoom);
            });
    }

    /**
     * Get or create chat room for an exchange request
     */
    @Transactional
    public ChatRoom getChatRoomForExchange(Long exchangeRequestId) {
        ExchangeRequest exchange = exchangeRequestRepository.findById(exchangeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found with id: " + exchangeRequestId));

        return chatRoomRepository.findByExchangeRequest_Id(exchangeRequestId)
            .orElseGet(() -> createChatRoomForExchange(exchange));
    }

    /**
     * Validate that current user is part of the exchange (either requester or book owner)
     */
    private void validateUserIsInExchange(ExchangeRequest exchange, User user) {
        long userId = user.getId();
        long requesterId = exchange.getRequester().getId();
        long ownerId = exchange.getOwner() != null
            ? exchange.getOwner().getId()
            : exchange.getBook().getOwner().getId();

        if (userId != requesterId && userId != ownerId) {
            throw new UnauthorizedActionException("You are not part of this exchange");
        }
    }

    /**
     * Get the other participant in the exchange (the one who is not the current user)
     */
    private User getOtherParticipant(ExchangeRequest exchange, User currentUser) {
        long currentUserId = currentUser.getId();
        if (currentUserId == exchange.getRequester().getId()) {
            return exchange.getOwner() != null ? exchange.getOwner() : exchange.getBook().getOwner();
        } else {
            return exchange.getRequester();
        }
    }

    /**
     * Get messages for a chat room
     * Access control: Only users in the exchange can see messages
     */
    @Transactional
    public List<ChatMessageResponse> getChatRoomMessages(Long exchangeRequestId) {
        ExchangeRequest exchange = exchangeRequestRepository.findById(exchangeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found"));

        User currentUser = userService.getCurrentUserEntity();
        validateUserIsInExchange(exchange, currentUser);

        ChatRoom chatRoom = getChatRoomForExchange(exchangeRequestId);

        List<ChatMessage> messages = chatMessageRepository.findByChatRoom_IdOrderByCreatedAtAsc(chatRoom.getId());

        return messages.stream()
            .map(msg -> new ChatMessageResponse(
                msg.getId(),
                msg.getSender().getId(),
                msg.getSender().getUsername(),
                msg.getContent(),
                msg.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Send a message to a chat room
     * Access control: Only users in the exchange can send messages
     */
    @Transactional
    public ChatMessageResponse sendMessageToRoom(Long exchangeRequestId, String messageContent) {
        ExchangeRequest exchange = exchangeRequestRepository.findById(exchangeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found"));

        User sender = userService.getCurrentUserEntity();
        return sendMessageToRoomInternal(exchange, sender, messageContent);
    }

    /**
     * Send a message as a specific authenticated username (used by WebSocket flow).
     */
    @Transactional
    public ChatMessageResponse sendMessageToRoomAsUser(Long exchangeRequestId,
                                                       String messageContent,
                                                       String username) {
        ExchangeRequest exchange = exchangeRequestRepository.findById(exchangeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found"));

        User sender = userService.findByUsername(username);
        return sendMessageToRoomInternal(exchange, sender, messageContent);
    }

    private ChatMessageResponse sendMessageToRoomInternal(ExchangeRequest exchange,
                                                          User sender,
                                                          String messageContent) {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        if (messageContent.length() > 1200) {
            throw new IllegalArgumentException("Message cannot exceed 1200 characters");
        }

        validateUserIsInExchange(exchange, sender);

        ChatRoom chatRoom = getChatRoomForExchange(exchange.getId());

        ChatMessage message = new ChatMessage();
        message.setChatRoom(chatRoom);
        message.setSender(sender);
        message.setContent(messageContent.trim());

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Update chat room's last message time
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        return new ChatMessageResponse(
            savedMessage.getId(),
            savedMessage.getSender().getId(),
            savedMessage.getSender().getUsername(),
            savedMessage.getContent(),
            savedMessage.getCreatedAt()
        );
    }

    /**
     * Get all active chat rooms for the current user
     * Returns exchanges where user is either requester or book owner
     */
    @Transactional(readOnly = true)
    public List<ExchangeChatRoomResponse> getActiveChatsForCurrentUser() {
        User currentUser = userService.getCurrentUserEntity();
        Long currentUserId = currentUser.getId();

        // Get all exchanges where user is involved
        List<ExchangeRequest> myExchanges = new ArrayList<>();

        List<ExchangeRequest> asRequester = exchangeRequestRepository.findByRequester_IdOrderByCreatedAtDesc(currentUserId);
        List<ExchangeRequest> asOwner = exchangeRequestRepository.findByOwner_IdOrderByCreatedAtDesc(currentUserId);

        myExchanges.addAll(asRequester);
        myExchanges.addAll(asOwner);

        Map<Long, ExchangeRequest> uniqueExchangesById = new LinkedHashMap<>();
        for (ExchangeRequest exchange : myExchanges) {
            uniqueExchangesById.put(exchange.getId(), exchange);
        }

        // Convert to chat room responses
        return uniqueExchangesById.values().stream()
            .filter(exchange -> !exchange.getStatus().equals(ExchangeRequest.Status.CANCELLED))
            .map(exchange -> convertToChatRoomResponse(exchange, currentUser))
            .sorted(Comparator
                .comparing(ExchangeChatRoomResponse::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExchangeChatRoomResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    /**
     * Convert an exchange to a chat room response
     */
    private ExchangeChatRoomResponse convertToChatRoomResponse(ExchangeRequest exchange, User currentUser) {
        ChatRoom chatRoom = chatRoomRepository.findByExchangeRequest_Id(exchange.getId())
            .orElse(null);

        Long roomId = chatRoom != null ? chatRoom.getId() : null;

        User otherUser = getOtherParticipant(exchange, currentUser);

        // Get last message
        ChatMessageResponse lastMessage = null;
        if (chatRoom != null) {
            List<ChatMessage> messages = chatMessageRepository.findByChatRoom_IdOrderByCreatedAtDesc(chatRoom.getId());
            if (!messages.isEmpty()) {
                ChatMessage msg = messages.get(0);
                lastMessage = new ChatMessageResponse(
                    msg.getId(),
                    msg.getSender().getId(),
                    msg.getSender().getUsername(),
                    msg.getContent(),
                    msg.getCreatedAt()
                );
            }
        }

        return new ExchangeChatRoomResponse(
            roomId,
            exchange.getId(),
            otherUser.getId(),
            otherUser.getUsername(),
            exchange.getBook().getId(),
            exchange.getBook().getTitle(),
            exchange.getBook().getAuthor(),
            exchange.getOfferedBook().getId(),
            exchange.getOfferedBook().getTitle(),
            exchange.getOfferedBook().getAuthor(),
            exchange.getStatus().toString(),
            lastMessage != null ? lastMessage.getContent() : null,
            lastMessage != null ? lastMessage.getSenderUsername() : null,
            lastMessage != null ? lastMessage.getTimestamp() : null,
            chatRoom != null ? chatRoom.getCreatedAt() : null
        );
    }

    /**
     * Check if current user can access a chat room for an exchange
     */
    @Transactional(readOnly = true)
    public boolean canUserAccessExchangeChat(Long exchangeRequestId) {
        try {
            ExchangeRequest exchange = exchangeRequestRepository.findById(exchangeRequestId)
                .orElse(null);

            if (exchange == null) {
                return false;
            }

            User currentUser = userService.getCurrentUserEntity();
            validateUserIsInExchange(exchange, currentUser);
            return true;
        } catch (UnauthorizedActionException | ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if a specific username can access a chat room for an exchange.
     */
    @Transactional(readOnly = true)
    public boolean canUserAccessExchangeChat(Long exchangeRequestId, String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        try {
            ExchangeRequest exchange = exchangeRequestRepository.findById(exchangeRequestId)
                .orElse(null);

            if (exchange == null) {
                return false;
            }

            User user = userService.findByUsername(username);
            validateUserIsInExchange(exchange, user);
            return true;
        } catch (UnauthorizedActionException | ResourceNotFoundException e) {
            return false;
        }
    }
}
