package es.urjc.code.backend.dto.request;

import jakarta.validation.constraints.*;

public class TeamCreateRequest {
    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    private String university;

    @NotBlank
    private String mainGame;

    private String description;

    @NotBlank
    @Size(min = 2, max = 5)
    private String tag;

    public TeamCreateRequest() {}

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
}
