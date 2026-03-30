package es.urjc.code.backend.model;

import jakarta.persistence.*;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private String nickname;
    private String email;
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles;

    @Lob
    private Blob imageFile;

    @ManyToMany(mappedBy = "players")
    private List<Team> teams = new ArrayList<>();

    @ManyToMany
    private List<Tournament> favoriteTournaments = new ArrayList<>();

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Tournament> createdTournaments = new ArrayList<>();

    public User() {}

    public User(String name, String nickname, String email, String password, List<String> roles) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }
    public List<Tournament> getCreatedTournaments() {
        return createdTournaments;
    }
    public void setCreatedTournaments(List<Tournament> createdTournaments) {
        this.createdTournaments = createdTournaments;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public List<Tournament> getFavoriteTournaments() {
        return favoriteTournaments;
    }
    public void setFavoriteTournaments(List<Tournament> favoriteTournaments) {
        this.favoriteTournaments = favoriteTournaments;
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
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public List<String> getRoles() {
        return roles;
    }
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
    public List<Team> getTeams() {
        return teams;
    }
    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

}