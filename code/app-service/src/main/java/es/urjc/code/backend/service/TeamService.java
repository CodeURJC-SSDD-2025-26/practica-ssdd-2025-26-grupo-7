package es.urjc.code.backend.service;

import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.Tournament;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.MatchRepository;
import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.TournamentRepository;
import es.urjc.code.backend.repository.UserRepository;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    public List<Team> findAll() {
        return teamRepository.findAll();
    }

    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id);
    }

    public long count() {
        return teamRepository.count();
    }

    public Team save(Team team) {
        return teamRepository.save(team);
    }

    @Transactional
    public Team loadTeamWithStats(Long id) {
        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isEmpty())
            return null;

        Team team = opt.get();
        List<Match> teamMatches = matchRepository.findByLocalTeamOrAwayTeam(team, team);
        int wins = 0, losses = 0, played = 0;

        for (Match m : teamMatches) {
            if ("Finalizado".equals(m.getState())) {
                played++;
                boolean isLocal = m.getLocalTeam().getId().equals(id);
                int scoreM = isLocal ? m.getScoreLocal() : m.getScoreAway();
                int scoreO = isLocal ? m.getScoreAway() : m.getScoreLocal();
                if (scoreM > scoreO)
                    wins++;
                else if (scoreM < scoreO)
                    losses++;
            }
        }

        team.setWins(wins);
        team.setLosses(losses);
        team.setMatchesPlayed(played);
        teamRepository.saveAndFlush(team);
        return team;
    }

    public List<Match> findTeamMatches(Team team) {
        return matchRepository.findByLocalTeamOrAwayTeam(team, team);
    }

    public Team createTeam(String name, String university, String mainGame,
            String description, String tag, Long captainId,
            MultipartFile imageFile, MultipartFile bannerFile,
            String currentUsername) throws IOException {

        Team team = new Team(name, university, mainGame, description);
        if (tag != null)
            team.setTag(tag);

        if (imageFile != null && !imageFile.isEmpty()) {
            team.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            team.setBannerFile(
                    BlobProxy.generateProxy(bannerFile.getInputStream(), bannerFile.getSize()));
        }

        User captain = null;
        if (captainId != null) {
            captain = userRepository.findById(captainId).orElse(null);
        }
        if (captain == null && currentUsername != null) {
            captain = userRepository.findByEmail(currentUsername).orElse(null);
        }

        if (captain != null) {
            team.setCaptain(captain);
            team.getPlayers().add(captain);
            teamRepository.save(team);
            captain.setTeam(team);
            userRepository.save(captain);
        } else {
            teamRepository.save(team);
        }

        return team;
    }

    public boolean editTeam(Long id, String name, String university, String mainGame,
            String description, String tag, Long captainId,
            MultipartFile imageFile, MultipartFile bannerFile) throws IOException {

        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isEmpty())
            return false;

        Team team = opt.get();
        team.setName(name);
        team.setUniversity(university);
        team.setMainGame(mainGame);
        team.setDescription(description);
        team.setTag(tag);

        if (captainId != null) {
            userRepository.findById(captainId).ifPresent(newCaptain -> {
                team.setCaptain(newCaptain);
                if (!team.getPlayers().contains(newCaptain)) {
                    Team oldTeam = newCaptain.getTeam();
                    if (oldTeam != null && !oldTeam.equals(team)) {
                        oldTeam.getPlayers().remove(newCaptain);
                        teamRepository.save(oldTeam);
                    }
                    newCaptain.setTeam(team);
                    userRepository.save(newCaptain);
                    team.getPlayers().add(newCaptain);
                }
            });
        } else {
            team.setCaptain(null);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            team.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }
        if (bannerFile != null && !bannerFile.isEmpty()) {
            team.setBannerFile(
                    BlobProxy.generateProxy(bannerFile.getInputStream(), bannerFile.getSize()));
        }

        teamRepository.save(team);
        return true;
    }

    @Transactional
    public void addPlayer(Long teamId, Long userId) {
        teamRepository.findById(teamId).ifPresent(team -> userRepository.findById(userId).ifPresent(user -> {
            if (user.getTeam() == null) {
                user.setTeam(team);
                team.getPlayers().add(user);
                userRepository.save(user);
                teamRepository.save(team);
            }
        }));
    }

    @Transactional
    public void removePlayer(Long teamId, Long userId) {
        teamRepository.findById(teamId).ifPresent(team -> userRepository.findById(userId).ifPresent(user -> {
            team.getPlayers().removeIf(p -> p.getId().equals(userId));
            if (team.getCaptain() != null && team.getCaptain().getId().equals(userId)) {
                team.setCaptain(null);
            }
            user.setTeam(null);
            userRepository.save(user);
            teamRepository.save(team);
        }));
    }

    @Transactional
    public void deleteTeam(Team team) {
        List<User> players = new ArrayList<>(team.getPlayers());
        for (User player : players) {
            player.setTeam(null);
            userRepository.save(player);
        }
        team.getPlayers().clear();
        teamRepository.saveAndFlush(team);

        List<Match> matches = matchRepository.findByLocalTeamOrAwayTeam(team, team);
        for (Match match : matches) {
            Tournament t = match.getTournament();
            if (t != null) {
                t.getMatches().remove(match);
                tournamentRepository.save(t);
            }
        }
        matchRepository.deleteAll(matches);
        matchRepository.flush();

        List<Tournament> tournaments = tournamentRepository.findByTeamsContaining(team);
        for (Tournament tournament : tournaments) {
            tournament.getTeams().remove(team);
            tournamentRepository.save(tournament);
        }
        tournamentRepository.flush();

        teamRepository.delete(team);
        teamRepository.flush();
    }

    public boolean isCaptainOf(Team team, String userEmail) {
        return team.getCaptain() != null
                && team.getCaptain().getEmail().equals(userEmail);
    }

    public List<Map<String, Object>> getTeamPlayersForApi(Long teamId) {
        Optional<Team> opt = teamRepository.findById(teamId);
        if (opt.isPresent()) {
            return opt.get().getPlayers().stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("nickname", p.getNickname());
                return map;
            }).toList();
        }
        return Collections.emptyList();
    }

    public List<Map<String, Object>> prepareAdminTeamsList() {
        return teamRepository.findAll().stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("university", t.getUniversity());
            map.put("mainGame", t.getMainGame());
            map.put("wins", t.getWins());
            map.put("losses", t.getLosses());
            map.put("points", t.getWins() * 3);

            int played = t.getMatchesPlayed();
            int winRate = played > 0 ? (int) Math.round((t.getWins() * 100.0) / played) : 0;
            map.put("winRate", winRate);

            return map;
        }).toList();
    }
}
