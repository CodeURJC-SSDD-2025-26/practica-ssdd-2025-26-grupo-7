package es.urjc.code.backend.dto.response;

import es.urjc.code.backend.model.Tournament;

public class TournamentResponse {
    private Long id;
    private String name;
    private String game;
    private String platform;
    private String mode;
    private int maxTeams;
    private String startDate;
    private String state;
    private String description;
    private String rules;
    private int teamCount;
    private int matchCount;

    public TournamentResponse() {}

    public TournamentResponse(Tournament t) {
        this.id = t.getId();
        this.name = t.getName();
        this.game = t.getGame();
        this.platform = t.getPlatform();
        this.mode = t.getMode();
        this.maxTeams = t.getMaxTeams();
        this.startDate = t.getStartDate();
        this.state = t.getState();
        this.description = t.getDescription();
        this.rules = t.getRules();
        this.teamCount = t.getTeams() != null ? t.getTeams().size() : 0;
        this.matchCount = t.getMatches() != null ? t.getMatches().size() : 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public int getTeamCount() { return teamCount; }
    public void setTeamCount(int teamCount) { this.teamCount = teamCount; }

    public int getMatchCount() { return matchCount; }
    public void setMatchCount(int matchCount) { this.matchCount = matchCount; }
}
