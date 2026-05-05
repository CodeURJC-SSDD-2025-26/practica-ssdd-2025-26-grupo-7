package es.urjc.code.backend.dto.response;

import es.urjc.code.backend.model.Match;

public class MatchResponse {
    private Long id;
    private String matchDate;
    private String phase;
    private String state;
    private String format;
    private String notes;
    private Integer scoreLocal;
    private Integer scoreAway;
    private String result;
    private Long tournamentId;
    private String tournamentName;
    private Long localTeamId;
    private String localTeamName;
    private Long awayTeamId;
    private String awayTeamName;

    public MatchResponse() {}

    public MatchResponse(Match m) {
        this.id = m.getId();
        this.matchDate = m.getMatchDate();
        this.phase = m.getPhase();
        this.state = m.getState();
        this.format = m.getFormat();
        this.notes = m.getNotes();
        this.scoreLocal = m.getScoreLocal();
        this.scoreAway = m.getScoreAway();
        this.result = m.getResult();
        if (m.getTournament() != null) {
            this.tournamentId = m.getTournament().getId();
            this.tournamentName = m.getTournament().getName();
        }
        if (m.getLocalTeam() != null) {
            this.localTeamId = m.getLocalTeam().getId();
            this.localTeamName = m.getLocalTeam().getName();
        }
        if (m.getAwayTeam() != null) {
            this.awayTeamId = m.getAwayTeam().getId();
            this.awayTeamName = m.getAwayTeam().getName();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatchDate() { return matchDate; }
    public void setMatchDate(String matchDate) { this.matchDate = matchDate; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getScoreLocal() { return scoreLocal; }
    public void setScoreLocal(Integer scoreLocal) { this.scoreLocal = scoreLocal; }

    public Integer getScoreAway() { return scoreAway; }
    public void setScoreAway(Integer scoreAway) { this.scoreAway = scoreAway; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Long getTournamentId() { return tournamentId; }
    public void setTournamentId(Long tournamentId) { this.tournamentId = tournamentId; }

    public String getTournamentName() { return tournamentName; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }

    public Long getLocalTeamId() { return localTeamId; }
    public void setLocalTeamId(Long localTeamId) { this.localTeamId = localTeamId; }

    public String getLocalTeamName() { return localTeamName; }
    public void setLocalTeamName(String localTeamName) { this.localTeamName = localTeamName; }

    public Long getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(Long awayTeamId) { this.awayTeamId = awayTeamId; }

    public String getAwayTeamName() { return awayTeamName; }
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }
}
