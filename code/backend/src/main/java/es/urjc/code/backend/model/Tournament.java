package es.urjc.code.backend.model;

import jakarta.persistence.*;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private String game;
    private String platform;
    private String mode;
    private int maxTeams;
    private String startDate;
    private String state;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Lob
    private Blob imageFile;

    @ManyToOne
    private User creator;

    @ManyToMany
    private List<Team> teams = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    private List<Match> matches = new ArrayList<>();

    public Tournament() {}

    public Tournament(String name, String game, String platform, String mode, int maxTeams, String startDate, String description, String rules) {
        this.name = name;
        this.game = game;
        this.platform = platform;
        this.mode = mode;
        this.maxTeams = maxTeams;
        this.startDate = startDate;
        this.description = description;
        this.rules = rules;
        this.state = "Próximamente";
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getGame() {
        return game;
    }
    public void setGame(String game) {
        this.game = game;
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
    public List<Match> getMatches() {
        return matches;
    }
    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }
    public int getMaxTeams() {
        return maxTeams;
    }
    public void setMaxTeams(int maxTeams) {
        this.maxTeams = maxTeams;
    }
    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPlatform() {
        return platform;
    }
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    public String getRules() {
        return rules;
    }
    public void setRules(String rules) {
        this.rules = rules;
    }
    public String getStartDate() {
        return startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public List<Team> getTeams() {
        return teams;
    }
    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }
}
    