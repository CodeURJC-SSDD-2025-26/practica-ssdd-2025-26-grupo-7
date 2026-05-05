package es.urjc.code.backend.dto.request;

public class MatchCreateRequest {
    private Long tournamentId;
    private Long localTeamId;
    private Long awayTeamId;
    private String phase;
    private String format;
    private String matchDate;
    private String notes;

    public MatchCreateRequest() {}

    // Getters and Setters
    public Long getTournamentId() { return tournamentId; }
    public void setTournamentId(Long tournamentId) { this.tournamentId = tournamentId; }

    public Long getLocalTeamId() { return localTeamId; }
    public void setLocalTeamId(Long localTeamId) { this.localTeamId = localTeamId; }

    public Long getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(Long awayTeamId) { this.awayTeamId = awayTeamId; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getMatchDate() { return matchDate; }
    public void setMatchDate(String matchDate) { this.matchDate = matchDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
