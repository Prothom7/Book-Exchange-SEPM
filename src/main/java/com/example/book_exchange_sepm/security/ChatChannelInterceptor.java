package com.example.book_exchange_sepm.security;

import com.example.book_exchange_sepm.service.CustomUserDetailsService;
import com.example.book_exchange_sepm.service.ChatRoomService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class ChatChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ChatRoomService chatRoomService;

    public ChatChannelInterceptor(JwtUtil jwtUtil,
                                  CustomUserDetailsService userDetailsService,
                                  ChatRoomService chatRoomService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.chatRoomService = chatRoomService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (command == StompCommand.CONNECT) {
            if (accessor.getUser() != null) {
                return message;
            }

            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || authorization.isBlank()) {
                // Allow session-authenticated websocket handshakes that do not use JWT header.
                return message;
            }

            String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;

            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtUtil.validateToken(token, userDetails)) {
                throw new IllegalArgumentException("Invalid websocket token");
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
            accessor.setUser(authentication);
            return message;
        }

        if (command != StompCommand.SUBSCRIBE && command != StompCommand.SEND) {
            return message;
        }

        Principal principal = accessor.getUser();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new AccessDeniedException("Unauthenticated websocket action");
        }

        Long exchangeRequestId = extractExchangeRequestId(command, accessor.getDestination());
        if (exchangeRequestId == null) {
            return message;
        }

        if (!chatRoomService.canUserAccessExchangeChat(exchangeRequestId, principal.getName())) {
            throw new AccessDeniedException("You are not allowed to access this exchange chat");
        }

        return message;
    }

    private Long extractExchangeRequestId(StompCommand command, String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }

        if (command == StompCommand.SUBSCRIBE && destination.startsWith("/topic/exchange/")) {
            String value = destination.substring("/topic/exchange/".length());
            return parseLongOrNull(value);
        }

        if (command == StompCommand.SUBSCRIBE && destination.startsWith("/topic/chat/")) {
            String value = destination.substring("/topic/chat/".length());
            return parseLongOrNull(value);
        }

        if (command == StompCommand.SEND
                && destination.startsWith("/app/exchange/")
                && destination.endsWith("/send")) {
            String value = destination.substring("/app/exchange/".length(), destination.length() - "/send".length());
            return parseLongOrNull(value);
        }

        if (command == StompCommand.SEND
                && destination.startsWith("/app/chat/")
                && destination.endsWith("/send")) {
            String value = destination.substring("/app/chat/".length(), destination.length() - "/send".length());
            return parseLongOrNull(value);
        }

        return null;
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
