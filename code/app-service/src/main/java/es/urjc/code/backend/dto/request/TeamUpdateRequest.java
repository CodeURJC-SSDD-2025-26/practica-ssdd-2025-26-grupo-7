package es.urjc.code.backend.dto.request;

public class TeamUpdateRequest {
    private String name;
    private String university;
    private String mainGame;
    private String description;
    private String tag;
    private Long captainId;

    public TeamUpdateRequest() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getMainGame() { return mainGame; }
    public void setMainGame(String mainGame) { this.mainGame = mainGame; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public Long getCaptainId() { return captainId; }
    public void setCaptainId(Long captainId) { this.captainId = captainId; }
}
