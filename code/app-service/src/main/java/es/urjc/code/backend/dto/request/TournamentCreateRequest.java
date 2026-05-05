package es.urjc.code.backend.dto.request;

import jakarta.validation.constraints.*;

public class TournamentCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String game;

    @NotBlank
    private String platform;

    @NotBlank
    private String mode;

    @Min(2)
    private int maxTeams;

    @NotBlank
    private String startDate;

    private String description;
    private String rules;

    public TournamentCreateRequest() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public int getMaxTeams() { return maxTeams; }
    public void setMaxTeams(int maxTeams) { this.maxTeams = maxTeams; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }
}
