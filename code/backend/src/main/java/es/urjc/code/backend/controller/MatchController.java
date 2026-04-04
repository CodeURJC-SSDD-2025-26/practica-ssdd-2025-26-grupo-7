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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
        model.addAttribute("teams", teamRepository.findAll());

        if (match.getMatchDate() != null && match.getMatchDate().contains(" ")) {
            String[] parts = match.getMatchDate().split(" ");
            model.addAttribute("matchDay", parts[0]);
            model.addAttribute("matchTime", parts[1]);
        }

        return "edit-matches";
    }

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
            @RequestParam(required = false) String notes) {

        Optional<Match> matchOpt = matchRepository.findById(id);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            match.setLocalTeam(teamRepository.findById(localTeamId).orElseThrow());
            match.setAwayTeam(teamRepository.findById(awayTeamId).orElseThrow());
            match.setPhase(phase);
            match.setFormat(format);
            match.setState(state);
            match.setMatchDate(date + " " + time);
            match.setScoreLocal(scoreLocal);
            match.setScoreAway(scoreAway);
            match.setNotes(notes);

            if ("Finalizado".equals(state)) {
                match.setResult(scoreLocal + " - " + scoreAway);
            }

            matchRepository.save(match);
        }
        return "redirect:/admin";
    }

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

        statsRepository.deleteByMatch(match);

        if (match.getLocalTeam() != null) {
            for (User player : match.getLocalTeam().getPlayers()) {
                String pId = player.getId().toString();
                if (allParams.containsKey("kills_" + pId)) {
                    PlayerMatchStats s = new PlayerMatchStats(match, player,
                            Integer.parseInt(allParams.getOrDefault("kills_" + pId, "0")),
                            Integer.parseInt(allParams.getOrDefault("deaths_" + pId, "0")),
                            Integer.parseInt(allParams.getOrDefault("assists_" + pId, "0")),
                            Integer.parseInt(allParams.getOrDefault("acs_" + pId, "0")));
                    statsRepository.save(s);
                }
            }
        }

        if (match.getAwayTeam() != null) {
            for (User player : match.getAwayTeam().getPlayers()) {
                String pId = player.getId().toString();
                if (allParams.containsKey("kills_" + pId)) {
                    PlayerMatchStats s = new PlayerMatchStats(match, player,
                            Integer.parseInt(allParams.getOrDefault("kills_" + pId, "0")),
                            Integer.parseInt(allParams.getOrDefault("deaths_" + pId, "0")),
                            Integer.parseInt(allParams.getOrDefault("assists_" + pId, "0")),
                            Integer.parseInt(allParams.getOrDefault("acs_" + pId, "0")));
                    statsRepository.save(s);
                }
            }
        }

        matchRepository.save(match);
        return "redirect:/matches/" + id;
    }
}
