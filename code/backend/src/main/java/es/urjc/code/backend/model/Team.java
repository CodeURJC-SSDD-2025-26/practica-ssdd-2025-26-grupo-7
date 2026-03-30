package es.urjc.code.backend.model;

import jakarta.persistence.*;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private String university;
    private String mainGame;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    private Blob imageFile;

    private int matchesPlayed;
    private int wins;
    private int losses;

    @ManyToMany
    private List<User> players = new ArrayList<>();

    @ManyToOne 
    private User captain;

    public Team() {}

    public Team(String name, String university, String mainGame, String description) {
        this.name = name;
        this.university = university;
        this.mainGame = mainGame;
        this.description = description;
        this.matchesPlayed = 0;
        this.wins = 0;
        this.losses = 0;
    }
    public User getCaptain() {
        return captain;
    }
    public void setCaptain(User captain) {
        this.captain = captain;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Blob getImageFile() {
        return imageFile;
    }
    public void setImageFile(Blob imageFile) {
        this.imageFile = imageFile;
    }
    public int getLosses() {
        return losses;
    }
    public void setLosses(int losses) {
        this.losses = losses;
    }
    public String getMainGame() {
        return mainGame;
    }
    public void setMainGame(String mainGame) {
        this.mainGame = mainGame;
    }
    public int getMatchesPlayed() {
        return matchesPlayed;
    }
    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<User> getPlayers() {
        return players;
    }
    public void setPlayers(List<User> players) {
        this.players = players;
    }
    public String getUniversity() {
        return university;
    }
    public void setUniversity(String university) {
        this.university = university;
    }
    public int getWins() {
        return wins;
    }
    public void setWins(int wins) {
        this.wins = wins;
    }
    
}