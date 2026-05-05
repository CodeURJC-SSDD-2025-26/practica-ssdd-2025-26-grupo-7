package es.urjc.code.backend.dto.request;

public class TournamentUpdateRequest {
    private String name;
    private String game;
    private String platform;
    private String mode;
    private Integer maxTeams;
    private String startDate;
    private String description;
    private String rules;
    private String state;

    public TournamentUpdateRequest() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Integer getMaxTeams() { return maxTeams; }
    public void setMaxTeams(Integer maxTeams) { this.maxTeams = maxTeams; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
