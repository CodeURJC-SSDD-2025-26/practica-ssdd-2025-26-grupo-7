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

    @Column(unique = true)
    private String email;

    private String password;

    private String university;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles;

    @Lob
    private Blob imageFile;

    @ManyToOne
    private Team team;

    @ManyToMany
    private List<Tournament> favoriteTournaments = new ArrayList<>();

    public User() {
    }

    public User(String name, String nickname, String email, String university, String password, List<String> roles) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.university = university;
        this.password = password;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUniversity() {
        return university != null ? university : "";
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Transient
    public boolean getIsAdmin() {
        return roles != null && roles.contains("ADMIN");
    }

    @Transient
    public boolean getHasTeam() {
        return team != null;
    }

    @Transient
    public boolean getIsCaptain() {
        return team != null && team.getCaptain() != null && this.id != null
                && this.id.equals(team.getCaptain().getId());
    }

    @Transient
    public String getAvatarInitials() {
        if (nickname != null && nickname.length() >= 2) {
            return nickname.substring(0, 2).toUpperCase();
        } else if (nickname != null && nickname.length() == 1) {
            return nickname.toUpperCase();
        }
        return "U";
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Blob getImageFile() {
        return imageFile;
    }

    public void setImageFile(Blob imageFile) {
        this.imageFile = imageFile;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public List<Tournament> getFavoriteTournaments() {
        return favoriteTournaments;
    }

    public void setFavoriteTournaments(List<Tournament> favoriteTournaments) {
        this.favoriteTournaments = favoriteTournaments;
    }
}
