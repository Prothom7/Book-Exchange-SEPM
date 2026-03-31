package com.example.book_exchange_sepm.service;

import org.springframework.stereotype.Service;

/**
 * DEPRECATED: This service is replaced by ChatRoomService for exchange-based chats.
 * Kept for backward compatibility only.
 * All new chat functionality uses ChatRoomService which provides context-based
 * messaging tied to specific book exchanges.
 */
@Service
@Deprecated(since = "2.0.0", forRemoval = true)
public class ChatService {
    // Deprecated - use ChatRoomService instead
}

