package es.urjc.code.backend.dto.response;

import es.urjc.code.backend.model.Message;

public class MessageResponse {
    private Long id;
    private String subject;
    private String content;
    private String sentAt;
    private boolean isRead;
    private Long senderId;
    private String senderName;
    private Long recipientId;
    private String recipientName;

    public MessageResponse() {}

    public MessageResponse(Message m) {
        this.id = m.getId();
        this.subject = m.getSubject();
        this.content = m.getContent();
        this.sentAt = m.getFormattedDate();
        this.isRead = m.isRead();
        if (m.getSender() != null) {
            this.senderId = m.getSender().getId();
            this.senderName = m.getSender().getNickname();
        }
        if (m.getRecipient() != null) {
            this.recipientId = m.getRecipient().getId();
            this.recipientName = m.getRecipient().getNickname();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
}
