package es.urjc.code.backend.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String matchDate;
    private String phase;
    private String state;
    private String format;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private Integer scoreLocal;
    private Integer scoreAway;
    private String result;

    @ManyToOne
    private Tournament tournament;

    @ManyToOne
    private Team localTeam;

    @ManyToOne
    private Team awayTeam;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerMatchStats> playerStats = new ArrayList<>();

    public Match() {}

    public Match(String matchDate, String phase, Tournament tournament, Team localTeam, Team awayTeam, String format) {
        this.matchDate = matchDate;
        this.phase = phase;
        this.tournament = tournament;
        this.localTeam = localTeam;
        this.awayTeam = awayTeam;
        this.format = format;
        this.state = "Programado";
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(Team awayTeam) {
        this.awayTeam = awayTeam;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Team getLocalTeam() {
        return localTeam;
    }

    public void setLocalTeam(Team localTeam) {
        this.localTeam = localTeam;
    }

    public String getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(String matchDate) {
        this.matchDate = matchDate;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getScoreLocal() {
        return scoreLocal != null ? scoreLocal : 0;
    }

    public void setScoreLocal(Integer scoreLocal) {
        this.scoreLocal = scoreLocal;
    }

    public Integer getScoreAway() {
        return scoreAway != null ? scoreAway : 0;
    }

    public void setScoreAway(Integer scoreAway) {
        this.scoreAway = scoreAway;
    }

    public List<PlayerMatchStats> getPlayerStats() {
        return playerStats;
    }

    public void setPlayerStats(List<PlayerMatchStats> playerStats) {
        this.playerStats = playerStats;
    }
}