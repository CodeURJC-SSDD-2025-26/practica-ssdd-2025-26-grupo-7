package es.urjc.code.backend.dto.response;

import es.urjc.code.backend.model.Team;
import java.util.List;

public class TeamResponse {
    private Long id;
    private String name;
    private String university;
    private String mainGame;
    private String tag;
    private String description;
    private int wins;
    private int losses;
    private int matchesPlayed;
    private int playerCount;
    private Long captainId;
    private String captainNickname;
    private List<PlayerSummary> players;

    public TeamResponse() {}

    public TeamResponse(Team team) {
        this.id = team.getId();
        this.name = team.getName();
        this.university = team.getUniversity();
        this.mainGame = team.getMainGame();
        this.tag = team.getTag();
        this.description = team.getDescription();
        this.wins = team.getWins();
        this.losses = team.getLosses();
        this.matchesPlayed = team.getMatchesPlayed();
        this.playerCount = team.getPlayers() != null ? team.getPlayers().size() : 0;
        if (team.getCaptain() != null) {
            this.captainId = team.getCaptain().getId();
            this.captainNickname = team.getCaptain().getNickname();
        }
        if (team.getPlayers() != null) {
            this.players = team.getPlayers().stream()
                    .map(p -> new PlayerSummary(p.getId(), p.getNickname(), p.getName()))
                    .toList();
        }
    }

    public static class PlayerSummary {
        private Long id;
        private String nickname;
        private String name;

        public PlayerSummary(Long id, String nickname, String name) {
            this.id = id;
            this.nickname = nickname;
            this.name = name;
        }

        // Getters
        public Long getId() { return id; }
        public String getNickname() { return nickname; }
        public String getName() { return name; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getMainGame() { return mainGame; }
    public void setMainGame(String mainGame) { this.mainGame = mainGame; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }

    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }

    public Long getCaptainId() { return captainId; }
    public void setCaptainId(Long captainId) { this.captainId = captainId; }

    public String getCaptainNickname() { return captainNickname; }
    public void setCaptainNickname(String captainNickname) { this.captainNickname = captainNickname; }

    public List<PlayerSummary> getPlayers() { return players; }
    public void setPlayers(List<PlayerSummary> players) { this.players = players; }
}
