package es.urjc.code.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String matchDate;
    private String phase;
    private String state;
    private String result;

    @ManyToOne
    private Tournament tournament;

    @ManyToOne
    private Team localTeam;

    @ManyToOne
    private Team awayTeam;

    public Match() {}

    public Match(String matchDate, String phase, Tournament tournament, Team localTeam, Team awayTeam) {
        this.matchDate = matchDate;
        this.phase = phase;
        this.tournament = tournament;
        this.localTeam = localTeam;
        this.awayTeam = awayTeam;
        this.state = "Pendiente";
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
    
}