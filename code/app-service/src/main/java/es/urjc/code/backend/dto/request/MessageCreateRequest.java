package es.urjc.code.backend.dto.request;

public class MessageCreateRequest {
    private Long recipientId;
    private String subject;
    private String content;

    public MessageCreateRequest() {}

    // Getters and Setters
    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
