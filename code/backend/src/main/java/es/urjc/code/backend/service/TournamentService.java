package es.urjc.code.backend.service;

import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.Tournament;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.TournamentRepository;
import es.urjc.code.backend.repository.UserRepository;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TournamentService {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Tournament> findAll() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> findById(Long id) {
        return tournamentRepository.findById(id);
    }

    public Page<Tournament> findWithFilters(String search, String game, String state, Pageable pageable) {
        return tournamentRepository.findWithFilters(search, game, state, pageable);
    }

    public Map<String, Object> enrichTournament(Tournament t, User currentUser) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("game", t.getGame());
        m.put("platform", t.getPlatform());
        m.put("mode", t.getMode());
        m.put("maxTeams", t.getMaxTeams());
        m.put("startDate", t.getStartDate());
        m.put("description", t.getDescription());
        m.put("rules", t.getRules());
        m.put("state", t.getState());
        m.put("stateActive", "En Curso".equals(t.getState()));
        m.put("stateUpcoming", "Próximamente".equals(t.getState()));
        m.put("stateFinished", "Finalizado".equals(t.getState()));
        m.put("isFavourite", currentUser != null && currentUser.getFavoriteTournaments().contains(t));
        return m;
    }

    public Map<String, Object> enrichMatch(Match match) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", match.getId());
        m.put("matchDate", match.getMatchDate());
        m.put("phase", match.getPhase());
        m.put("format", match.getFormat());
        m.put("state", match.getState());
        m.put("scoreLocal", match.getScoreLocal());
        m.put("scoreAway", match.getScoreAway());
        m.put("result", match.getResult());
        m.put("notes", match.getNotes());

        String state = match.getState() != null ? match.getState() : "";
        m.put("matchLive", "En Vivo".equals(state));
        m.put("matchFinished", "Finalizado".equals(state));
        m.put("matchScheduled", "Programado".equals(state));
        m.put("matchCancelled", "Cancelado".equals(state));

        if (match.getLocalTeam() != null) {
            Team lt = match.getLocalTeam();
            Map<String, Object> ltMap = new LinkedHashMap<>();
            ltMap.put("id", lt.getId());
            ltMap.put("name", lt.getName());
            m.put("localTeam", ltMap);
        }

        if (match.getAwayTeam() != null) {
            Team at = match.getAwayTeam();
            Map<String, Object> atMap = new LinkedHashMap<>();
            atMap.put("id", at.getId());
            atMap.put("name", at.getName());
            m.put("awayTeam", atMap);
        }

        return m;
    }

    public Tournament createTournament(String name, String game, String platform,
            String mode, int maxTeams, String startDate,
            String description, String rules,
            MultipartFile imageFile) throws IOException {

        Tournament t = new Tournament(name, game, platform, mode, maxTeams, startDate, description, rules);
        if (imageFile != null && !imageFile.isEmpty()) {
            t.setImageFile(BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }
        return tournamentRepository.save(t);
    }

    public boolean editTournament(Long id, String name, String game, String platform,
            String mode, int maxTeams, String startDate,
            String description, String rules, String state,
            MultipartFile imageFile) throws IOException {

        Optional<Tournament> opt = tournamentRepository.findById(id);
        if (opt.isEmpty())
            return false;

        Tournament t = opt.get();
        t.setName(name);
        t.setGame(game);
        t.setPlatform(platform);
        t.setMode(mode);
        t.setMaxTeams(maxTeams);
        t.setStartDate(startDate);
        t.setDescription(description);
        t.setRules(rules);
        t.setState(state);
        if (imageFile != null && !imageFile.isEmpty()) {
            t.setImageFile(BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }
        tournamentRepository.save(t);
        return true;
    }

    public void deleteTournament(Long id) {
        tournamentRepository.deleteById(id);
    }

    public void addTeamToTournament(Long tournamentId, Long teamId) {
        tournamentRepository.findById(tournamentId).ifPresent(t -> teamRepository.findById(teamId).ifPresent(team -> {
            if (!t.getTeams().contains(team)) {
                t.getTeams().add(team);
                tournamentRepository.save(t);
            }
        }));
    }

    public void removeTeamFromTournament(Long tournamentId, Long teamId) {
        tournamentRepository.findById(tournamentId).ifPresent(t -> teamRepository.findById(teamId).ifPresent(team -> {
            t.getTeams().remove(team);
            tournamentRepository.save(t);
        }));
    }

    public boolean toggleFavorite(Long tournamentId, User user) {
        Optional<Tournament> optTour = tournamentRepository.findById(tournamentId);
        if (user == null || optTour.isEmpty())
            return false;

        Tournament t = optTour.get();
        if (user.getFavoriteTournaments().contains(t)) {
            user.getFavoriteTournaments().remove(t);
        } else {
            user.getFavoriteTournaments().add(t);
        }
        userRepository.save(user);
        return true;
    }

    public List<Map<String, Object>> calculateRankedTeams(Tournament tournament) {
        List<Match> tournamentMatches = tournament.getMatches();
        Map<Long, int[]> statsMap = new HashMap<>();
        for (Team t : tournament.getTeams()) {
            statsMap.put(t.getId(), new int[] { 0, 0, 0 });
        }

        for (Match m : tournamentMatches) {
            if ("Finalizado".equals(m.getState())) {
                Team local = m.getLocalTeam();
                Team away = m.getAwayTeam();

                if (local != null && statsMap.containsKey(local.getId())) {
                    int[] s = statsMap.get(local.getId());
                    s[2]++;
                    if (m.getScoreLocal() > m.getScoreAway())
                        s[0]++;
                    else if (m.getScoreLocal() < m.getScoreAway())
                        s[1]++;
                }
                if (away != null && statsMap.containsKey(away.getId())) {
                    int[] s = statsMap.get(away.getId());
                    s[2]++;
                    if (m.getScoreAway() > m.getScoreLocal())
                        s[0]++;
                    else if (m.getScoreAway() < m.getScoreLocal())
                        s[1]++;
                }
            }
        }

        AtomicInteger rank = new AtomicInteger(1);
        return tournament.getTeams().stream()
                .map(t -> {
                    int[] s = statsMap.get(t.getId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("wins", s[0]);
                    m.put("losses", s[1]);
                    m.put("matchesPlayed", s[2]);
                    m.put("points", s[0] * 3);
                    return m;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("points"), (int) a.get("points")))
                .map(m -> {
                    int r = rank.getAndIncrement();
                    m.put("rank", r);
                    m.put("rankIs1", r == 1);
                    m.put("rankIs2", r == 2);
                    m.put("rankIs3", r == 3);
                    m.put("rankGt3", r > 3);
                    return m;
                })
                .toList();
    }

    public List<Tournament> findActiveTournaments(int limit) {
        return tournamentRepository.findAll().stream()
                .filter(t -> "En Curso".equals(t.getState()))
                .limit(limit)
                .toList();
    }

    public long countActiveTournaments() {
        return tournamentRepository.findAll().stream()
                .filter(t -> "En Curso".equals(t.getState()))
                .count();
    }
}
