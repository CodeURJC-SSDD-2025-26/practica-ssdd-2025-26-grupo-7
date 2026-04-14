package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.model.PlayerMatchStats;
import es.urjc.code.backend.service.MatchService;
import es.urjc.code.backend.service.TeamService;
import es.urjc.code.backend.service.TournamentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class MatchController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private TeamService teamService;

    @GetMapping("/matches")
    public String listMatches(Model model) {
        model.addAttribute("matches", matchService.findAll());
        return "matches";
    }

    @GetMapping("/matches/{id}")
    public String matchDetail(@PathVariable Long id, Model model) {
        Optional<Match> matchOpt = matchService.findById(id);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            model.addAttribute("match", match);

            List<PlayerMatchStats> localStats = new ArrayList<>();
            List<PlayerMatchStats> awayStats = new ArrayList<>();

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
        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("teams", teamService.findAll());
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

        matchService.createMatch(tournamentId, localTeamId, awayTeamId, phase, format, date, time, notes);
        return "redirect:/admin";
    }

    @GetMapping("/admin/matches/edit")
    public String editMatchSelection(Model model) {
        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("allMatches", matchService.findAll());
        return "edit-matches";
    }

    @GetMapping("/admin/matches/edit/{id}")
    public String editMatchForm(@PathVariable Long id, Model model) {
        Optional<Match> matchOpt = matchService.findById(id);
        if (matchOpt.isEmpty())
            return "redirect:/admin/matches/edit";

        Match match = matchOpt.get();
        model.addAttribute("match", match);
        model.addAttribute("tournaments", tournamentService.findAll());
        if (match.getTournament() != null) {
            model.addAttribute("teams", match.getTournament().getTeams());
        } else {
            model.addAttribute("teams", Collections.emptyList());
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

        model.addAttribute("localPlayersStats", matchService.preparePlayersWithStats(match, match.getLocalTeam()));
        model.addAttribute("awayPlayersStats", matchService.preparePlayersWithStats(match, match.getAwayTeam()));

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
            @RequestParam(required = false) String notes,
            @RequestParam Map<String, String> allParams) {

        matchService.updateMatch(id, localTeamId, awayTeamId, phase, format, state,
                date, time, scoreLocal, scoreAway, notes, allParams);
        return "redirect:/admin";
    }

    @PostMapping("/admin/matches/delete/{id}")
    public String deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return "redirect:/admin";
    }

    @GetMapping("/admin/matches/{id}/report")
    public String reportStatsForm(@PathVariable Long id, Model model) {
        Match match = matchService.findById(id).orElseThrow();
        model.addAttribute("match", match);
        model.addAttribute("stats", matchService.findStatsByMatch(match));
        return "report-stats";
    }

    @PostMapping("/admin/matches/{id}/report")
    public String saveReport(
            @PathVariable Long id,
            @RequestParam Integer scoreLocal,
            @RequestParam Integer scoreAway,
            @RequestParam String state,
            @RequestParam(required = false) String notes,
            @RequestParam Map<String, String> allParams) {

        matchService.saveReport(id, scoreLocal, scoreAway, state, notes, allParams);
        return "redirect:/matches/" + id;
    }
}
