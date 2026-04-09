package es.urjc.code.backend.controller;

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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class MatchController {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerMatchStatsRepository statsRepository;

    @GetMapping("/matches")
    public String listMatches(Model model) {
        model.addAttribute("matches", matchRepository.findAll());
        return "matches";
    }

    @GetMapping("/matches/{id}")
    public String matchDetail(@PathVariable Long id, Model model) {
        Optional<Match> matchOpt = matchRepository.findById(id);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            model.addAttribute("match", match);
            java.util.List<PlayerMatchStats> localStats = new java.util.ArrayList<>();
            java.util.List<PlayerMatchStats> awayStats = new java.util.ArrayList<>();

            for (PlayerMatchStats s : match.getPlayerStats()) {
                if (match.getLocalTeam() != null && match.getLocalTeam().getPlayers().contains(s.getPlayer())) {
                    localStats.add(s);
                } else if (match.getAwayTeam() != null && match.getAwayTeam().getPlayers().contains(s.getPlayer())) {
                    awayStats.add(s);
                }
            }
            model.addAttribute("localStats", localStats);
            model.addAttribute("awayStats", awayStats);

            return "match-detail";
        }
        return "redirect:/matches";
    }

    @GetMapping("/admin/matches/create")
    public String createMatchForm(Model model) {
        model.addAttribute("tournaments", tournamentRepository.findAll());
        model.addAttribute("teams", teamRepository.findAll());
        return "create-matches";
    }

    @PostMapping("/admin/matches/create")
    public String createMatch(
            @RequestParam Long tournamentId,
            @RequestParam Long localTeamId,
            @RequestParam Long awayTeamId,
            @RequestParam String phase,
            @RequestParam String format,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam(required = false) String notes) {

        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow();
        Team localTeam = teamRepository.findById(localTeamId).orElseThrow();
        Team awayTeam = teamRepository.findById(awayTeamId).orElseThrow();

        Match match = new Match(date + " " + time, phase, tournament, localTeam, awayTeam, format);
        match.setNotes(notes);
        matchRepository.save(match);

        return "redirect:/admin";
    }

    @GetMapping("/admin/matches/edit")
    public String editMatchSelection(Model model) {
        model.addAttribute("tournaments", tournamentRepository.findAll());
        model.addAttribute("allMatches", matchRepository.findAll());
        return "edit-matches";
    }

    @GetMapping("/admin/matches/edit/{id}")
    public String editMatchForm(@PathVariable Long id, Model model) {
        Optional<Match> matchOpt = matchRepository.findById(id);
        if (matchOpt.isEmpty())
            return "redirect:/admin/matches/edit";

        Match match = matchOpt.get();
        model.addAttribute("match", match);
        model.addAttribute("tournaments", tournamentRepository.findAll());
        if (match.getTournament() != null) {
            model.addAttribute("teams", match.getTournament().getTeams());
        } else {
            model.addAttribute("teams", java.util.Collections.emptyList());
        }

        if (match.getMatchDate() != null && match.getMatchDate().contains(" ")) {
            String[] parts = match.getMatchDate().split(" ");
            model.addAttribute("matchDay", parts[0]);
            model.addAttribute("matchTime", parts[1]);
        }

        String notes = match.getNotes();
        if (notes != null) {
            if (notes.length() > 5000)
                notes = notes.substring(0, 5000);
            if (notes.contains("<!DOCTYPE"))
                notes = "Content cleaned: HTML boilerplate detected";
            model.addAttribute("safeNotes", notes);
        } else {
            model.addAttribute("safeNotes", "");
        }

        model.addAttribute("localPlayersStats", preparePlayersWithStats(match, match.getLocalTeam()));
        model.addAttribute("awayPlayersStats", preparePlayersWithStats(match, match.getAwayTeam()));

        return "edit-matches";
    }

    private List<Map<String, Object>> preparePlayersWithStats(Match match, Team team) {
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

    @Transactional
    @PostMapping("/admin/matches/edit/{id}")
    public String updateMatch(
            @PathVariable Long id,
            @RequestParam Long localTeamId,
            @RequestParam Long awayTeamId,
            @RequestParam String phase,
            @RequestParam String format,
            @RequestParam String state,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam(required = false) Integer scoreLocal,
            @RequestParam(required = false) Integer scoreAway,
            @RequestParam(required = false) String notes,
            @RequestParam Map<String, String> allParams) {

        Optional<Match> matchOpt = matchRepository.findById(id);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();

            // Store old teams to update their stats later
            Team oldLocal = match.getLocalTeam();
            Team oldAway = match.getAwayTeam();

            // Update teams
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

            // Update player stats
            match.getPlayerStats().clear();
            processTeamStats(match, match.getLocalTeam(), allParams);
            processTeamStats(match, match.getAwayTeam(), allParams);

            matchRepository.save(match);
            
            // Update stats for all involved teams (old and new)
            if (oldLocal != null) updateTeamStats(oldLocal);
            if (oldAway != null) updateTeamStats(oldAway);
            if (match.getLocalTeam() != null && (oldLocal == null || !match.getLocalTeam().getId().equals(oldLocal.getId()))) {
                updateTeamStats(match.getLocalTeam());
            }
            if (match.getAwayTeam() != null && (oldAway == null || !match.getAwayTeam().getId().equals(oldAway.getId()))) {
                updateTeamStats(match.getAwayTeam());
            }
        }
        return "redirect:/admin";
    }

    private void processTeamStats(Match match, Team team, Map<String, String> allParams) {
        if (team == null)
            return;
        for (User player : team.getPlayers()) {
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
    }

    @Transactional
    @PostMapping("/admin/matches/delete/{id}")
    public String deleteMatch(@PathVariable Long id) {
        matchRepository.deleteById(id);
        return "redirect:/admin";
    }

    @GetMapping("/admin/matches/{id}/report")
    public String reportStatsForm(@PathVariable Long id, Model model) {
        Match match = matchRepository.findById(id).orElseThrow();
        model.addAttribute("match", match);

        List<PlayerMatchStats> existingStats = statsRepository.findByMatch(match);
        model.addAttribute("stats", existingStats);

        return "report-stats";
    }

    @Transactional
    @PostMapping("/admin/matches/{id}/report")
    public String saveReport(
            @PathVariable Long id,
            @RequestParam Integer scoreLocal,
            @RequestParam Integer scoreAway,
            @RequestParam String state,
            @RequestParam(required = false) String notes,
            @RequestParam java.util.Map<String, String> allParams) {

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
        }

        if (match.getAwayTeam() != null) {
            for (User player : match.getAwayTeam().getPlayers()) {
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
        }

        matchRepository.save(match);

        updateTeamStats(match.getLocalTeam());
        updateTeamStats(match.getAwayTeam());

        return "redirect:/matches/" + id;
    }

    private int parseSafely(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void updateTeamStats(Team team) {
        if (team == null)
            return;

        List<Match> matches = matchRepository.findByLocalTeamOrAwayTeam(team, team);
        int wins = 0;
        int losses = 0;
        int played = 0;

        for (Match m : matches) {
            if ("Finalizado".equals(m.getState())) {
                played++;
                boolean isLocal = m.getLocalTeam().getId().equals(team.getId());
                int myScore = isLocal ? m.getScoreLocal() : m.getScoreAway();
                int oppScore = isLocal ? m.getScoreAway() : m.getScoreLocal();

                if (myScore > oppScore) {
                    wins++;
                } else if (myScore < oppScore) {
                    losses++;
                }
            }
        }

        team.setWins(wins);
        team.setLosses(losses);
        team.setMatchesPlayed(played);
        teamRepository.saveAndFlush(team);
    }
}
