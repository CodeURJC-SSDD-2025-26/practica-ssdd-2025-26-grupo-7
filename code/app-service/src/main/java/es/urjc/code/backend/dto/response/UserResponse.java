package es.urjc.code.backend.dto.response;

import es.urjc.code.backend.model.User;
import java.util.List;

public class UserResponse {
    private Long id;
    private String name;
    private String nickname;
    private String email;
    private String university;
    private List<String> roles;
    private boolean enabled;
    private Long teamId;
    private String teamName;

    public UserResponse() {}

    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.university = user.getUniversity();
        this.roles = user.getRoles();
        this.enabled = user.isEnabled();
        if (user.getTeam() != null) {
            this.teamId = user.getTeam().getId();
            this.teamName = user.getTeam().getName();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
}
