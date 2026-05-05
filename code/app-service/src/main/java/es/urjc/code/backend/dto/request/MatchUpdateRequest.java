package es.urjc.code.backend.dto.request;

public class MatchUpdateRequest {
    private Long localTeamId;
    private Long awayTeamId;
    private String phase;
    private String format;
    private String state;
    private String matchDate;
    private Integer scoreLocal;
    private Integer scoreAway;
    private String notes;

    public MatchUpdateRequest() {}

    // Getters and Setters
    public Long getLocalTeamId() { return localTeamId; }
    public void setLocalTeamId(Long localTeamId) { this.localTeamId = localTeamId; }

    public Long getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(Long awayTeamId) { this.awayTeamId = awayTeamId; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getMatchDate() { return matchDate; }
    public void setMatchDate(String matchDate) { this.matchDate = matchDate; }

    public Integer getScoreLocal() { return scoreLocal; }
    public void setScoreLocal(Integer scoreLocal) { this.scoreLocal = scoreLocal; }

    public Integer getScoreAway() { return scoreAway; }
    public void setScoreAway(Integer scoreAway) { this.scoreAway = scoreAway; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
