package es.urjc.code.backend.model;

import jakarta.persistence.*;

@Entity
public class PlayerMatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Match match;

    @ManyToOne
    private User player;

    private int kills;
    private int deaths;
    private int assists;
    private int acs;

    public PlayerMatchStats() {}

    public PlayerMatchStats(Match match, User player, int kills, int deaths, int assists, int acs) {
        this.match = match;
        this.player = player;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.acs = acs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public User getPlayer() {
        return player;
    }

    public void setPlayer(User player) {
        this.player = player;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public int getAcs() {
        return acs;
    }

    public void setAcs(int acs) {
        this.acs = acs;
    }
    
    public double getKd() {
        if (deaths == 0) return kills;
        return Math.round((double) kills / deaths * 100.0) / 100.0;
    }
}
