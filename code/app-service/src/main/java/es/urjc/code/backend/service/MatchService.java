package es.urjc.code.backend.service;

import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.model.PlayerMatchStats;
import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.Tournament;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.MatchRepository;
import es.urjc.code.backend.repository.PlayerMatchStatsRepository;
import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.TournamentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerMatchStatsRepository statsRepository;

    public List<Match> findAll() {
        return matchRepository.findAll();
    }

    public org.springframework.data.domain.Page<Match> findWithFilters(Long tournamentId, String state, org.springframework.data.domain.Pageable pageable) {
        return matchRepository.findWithFilters(tournamentId, state, pageable);
    }

    public Optional<Match> findById(Long id) {
        return matchRepository.findById(id);
    }

    public long count() {
        return matchRepository.count();
    }

    public Match createMatch(Long tournamentId, Long localTeamId, Long awayTeamId,
            String phase, String format, String date, String time,
            String notes) {

        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow();
        Team localTeam = teamRepository.findById(localTeamId).orElseThrow();
        Team awayTeam = teamRepository.findById(awayTeamId).orElseThrow();

        Match match = new Match(date + " " + time, phase, tournament, localTeam, awayTeam, format);
        match.setNotes(notes);
        return matchRepository.save(match);
    }

    @Transactional
    public void updateMatch(Long id, Long localTeamId, Long awayTeamId,
            String phase, String format, String state,
            String date, String time,
            Integer scoreLocal, Integer scoreAway,
            String notes, Map<String, String> allParams) {

        Optional<Match> matchOpt = matchRepository.findById(id);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();

            Team oldLocal = match.getLocalTeam();
            Team oldAway = match.getAwayTeam();

            match.setLocalTeam(teamRepository.findById(localTeamId).orElseThrow());
            match.setAwayTeam(teamRepository.findById(awayTeamId).orElseThrow());
            match.setPhase(phase);
            match.setFormat(format);
            match.setState(state);
            match.setMatchDate(date + " " + time);
            match.setScoreLocal(scoreLocal != null ? scoreLocal : 0);
            match.setScoreAway(scoreAway != null ? scoreAway : 0);
            match.setNotes(notes);

            if ("Finalizado".equals(state)) {
                match.setResult(match.getScoreLocal() + " - " + match.getScoreAway());
            } else {
                match.setResult(null);
            }

            match.getPlayerStats().clear();
            processTeamStats(match, match.getLocalTeam(), allParams);
            processTeamStats(match, match.getAwayTeam(), allParams);

            matchRepository.save(match);

            if (oldLocal != null)
                updateTeamStats(oldLocal);
            if (oldAway != null)
                updateTeamStats(oldAway);
            if (match.getLocalTeam() != null
                    && (oldLocal == null || !match.getLocalTeam().getId().equals(oldLocal.getId()))) {
                updateTeamStats(match.getLocalTeam());
            }
            if (match.getAwayTeam() != null
                    && (oldAway == null || !match.getAwayTeam().getId().equals(oldAway.getId()))) {
                updateTeamStats(match.getAwayTeam());
            }
        }
    }

    @Transactional
    public void deleteMatch(Long id) {
        matchRepository.deleteById(id);
    }

    @Transactional
    public void saveReport(Long id, Integer scoreLocal, Integer scoreAway,
            String state, String notes, Map<String, String> allParams) {

        Match match = matchRepository.findById(id).orElseThrow();
        match.setScoreLocal(scoreLocal);
        match.setScoreAway(scoreAway);
        match.setState(state);
        match.setNotes(notes);

        if ("Finalizado".equals(state)) {
            match.setResult(scoreLocal + " - " + scoreAway);
        }

        match.getPlayerStats().clear();

        if (match.getLocalTeam() != null) {
            for (User player : match.getLocalTeam().getPlayers()) {
                addPlayerStatsFromParams(match, player, allParams);
            }
        }

        if (match.getAwayTeam() != null) {
            for (User player : match.getAwayTeam().getPlayers()) {
                addPlayerStatsFromParams(match, player, allParams);
            }
        }

        matchRepository.save(match);
        updateTeamStats(match.getLocalTeam());
        updateTeamStats(match.getAwayTeam());
    }

    public List<Map<String, Object>> preparePlayersWithStats(Match match, Team team) {
        if (team == null || team.getPlayers() == null)
            return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        List<PlayerMatchStats> allStats = match.getPlayerStats();

        for (User p : team.getPlayers()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("nickname", p.getNickname());

            PlayerMatchStats s = null;
            if (allStats != null) {
                for (PlayerMatchStats st : allStats) {
                    if (st.getPlayer() != null && st.getPlayer().getId().equals(p.getId())) {
                        s = st;
                        break;
                    }
                }
            }

            if (s != null) {
                map.put("kills", s.getKills());
                map.put("deaths", s.getDeaths());
                map.put("assists", s.getAssists());
                map.put("acs", s.getAcs());
            } else {
                map.put("kills", 0);
                map.put("deaths", 0);
                map.put("assists", 0);
                map.put("acs", 0);
            }
            result.add(map);
        }
        return result;
    }

    public List<PlayerMatchStats> findStatsByMatch(Match match) {
        return statsRepository.findByMatch(match);
    }

    public List<PlayerMatchStats> findStatsByPlayer(User player) {
        return statsRepository.findByPlayer(player);
    }

    private void processTeamStats(Match match, Team team, Map<String, String> allParams) {
        if (team == null)
            return;
        for (User player : team.getPlayers()) {
            addPlayerStatsFromParams(match, player, allParams);
        }
    }

    private void addPlayerStatsFromParams(Match match, User player, Map<String, String> allParams) {
        String pId = player.getId().toString();
        if (allParams.containsKey("kills_" + pId)) {
            PlayerMatchStats s = new PlayerMatchStats(match, player,
                    parseSafely(allParams.get("kills_" + pId), 0),
                    parseSafely(allParams.get("deaths_" + pId), 0),
                    parseSafely(allParams.get("assists_" + pId), 0),
                    parseSafely(allParams.get("acs_" + pId), 0));
            match.getPlayerStats().add(s);
        }
    }

    private void updateTeamStats(Team team) {
        if (team == null)
            return;

        List<Match> matches = matchRepository.findByLocalTeamOrAwayTeam(team, team);
        int wins = 0, losses = 0, played = 0;

        for (Match m : matches) {
            if ("Finalizado".equals(m.getState())) {
                played++;
                boolean isLocal = m.getLocalTeam().getId().equals(team.getId());
                int myScore = isLocal ? m.getScoreLocal() : m.getScoreAway();
                int oppScore = isLocal ? m.getScoreAway() : m.getScoreLocal();

                if (myScore > oppScore)
                    wins++;
                else if (myScore < oppScore)
                    losses++;
            }
        }

        team.setWins(wins);
        team.setLosses(losses);
        team.setMatchesPlayed(played);
        teamRepository.saveAndFlush(team);
    }

    private int parseSafely(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty())
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
