package es.urjc.code.backend.dto.request;

public class UserUpdateRequest {
    private String name;
    private String nickname;
    private String university;

    public UserUpdateRequest() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }
}
