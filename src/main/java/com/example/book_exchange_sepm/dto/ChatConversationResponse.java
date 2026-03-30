package com.example.book_exchange_sepm.dto;

import java.time.LocalDateTime;

public class ChatConversationResponse {

    private Long participantId;
    private String participantUsername;
    private String lastMessage;
    private LocalDateTime lastMessageTimestamp;
    private Boolean lastMessageSentByMe;

    public ChatConversationResponse() {
    }

    public ChatConversationResponse(Long participantId,
                                    String participantUsername,
                                    String lastMessage,
                                    LocalDateTime lastMessageTimestamp,
                                    Boolean lastMessageSentByMe) {
        this.participantId = participantId;
        this.participantUsername = participantUsername;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessageSentByMe = lastMessageSentByMe;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }

    public String getParticipantUsername() {
        return participantUsername;
    }

    public void setParticipantUsername(String participantUsername) {
        this.participantUsername = participantUsername;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(LocalDateTime lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public Boolean getLastMessageSentByMe() {
        return lastMessageSentByMe;
    }

    public void setLastMessageSentByMe(Boolean lastMessageSentByMe) {
        this.lastMessageSentByMe = lastMessageSentByMe;
    }
}
