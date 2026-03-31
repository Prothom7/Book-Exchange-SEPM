package com.example.book_exchange_sepm.dto;

import java.time.LocalDateTime;

/**
 * DTO for a chat room tied to an exchange request.
 * Shows the other participant and the exchange details.
 */
public class ExchangeChatRoomResponse {

    private Long roomId;
    private Long exchangeRequestId;
    private Long otherUserId;
    private String otherUserUsername;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private Long offeredBookId;
    private String offeredBookTitle;
    private String offeredBookAuthor;
    private String exchangeStatus;
    private String lastMessageContent;
    private String lastMessageSenderUsername;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    public ExchangeChatRoomResponse() {}

    public ExchangeChatRoomResponse(Long roomId, Long exchangeRequestId, Long otherUserId,
                                   String otherUserUsername, Long bookId, String bookTitle,
                                   String bookAuthor, Long offeredBookId, String offeredBookTitle,
                                   String offeredBookAuthor, String exchangeStatus,
                                   String lastMessageContent, String lastMessageSenderUsername,
                                   LocalDateTime lastMessageAt, LocalDateTime createdAt) {
        this.roomId = roomId;
        this.exchangeRequestId = exchangeRequestId;
        this.otherUserId = otherUserId;
        this.otherUserUsername = otherUserUsername;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.offeredBookId = offeredBookId;
        this.offeredBookTitle = offeredBookTitle;
        this.offeredBookAuthor = offeredBookAuthor;
        this.exchangeStatus = exchangeStatus;
        this.lastMessageContent = lastMessageContent;
        this.lastMessageSenderUsername = lastMessageSenderUsername;
        this.lastMessageAt = lastMessageAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getExchangeRequestId() {
        return exchangeRequestId;
    }

    public void setExchangeRequestId(Long exchangeRequestId) {
        this.exchangeRequestId = exchangeRequestId;
    }

    public Long getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(Long otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserUsername() {
        return otherUserUsername;
    }

    public void setOtherUserUsername(String otherUserUsername) {
        this.otherUserUsername = otherUserUsername;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public Long getOfferedBookId() {
        return offeredBookId;
    }

    public void setOfferedBookId(Long offeredBookId) {
        this.offeredBookId = offeredBookId;
    }

    public String getOfferedBookTitle() {
        return offeredBookTitle;
    }

    public void setOfferedBookTitle(String offeredBookTitle) {
        this.offeredBookTitle = offeredBookTitle;
    }

    public String getOfferedBookAuthor() {
        return offeredBookAuthor;
    }

    public void setOfferedBookAuthor(String offeredBookAuthor) {
        this.offeredBookAuthor = offeredBookAuthor;
    }

    public String getExchangeStatus() {
        return exchangeStatus;
    }

    public void setExchangeStatus(String exchangeStatus) {
        this.exchangeStatus = exchangeStatus;
    }

    public String getLastMessageContent() {
        return lastMessageContent;
    }

    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }

    public String getLastMessageSenderUsername() {
        return lastMessageSenderUsername;
    }

    public void setLastMessageSenderUsername(String lastMessageSenderUsername) {
        this.lastMessageSenderUsername = lastMessageSenderUsername;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
