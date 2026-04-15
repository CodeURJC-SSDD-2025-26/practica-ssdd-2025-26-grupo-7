package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Message;
import es.urjc.code.backend.model.PlayerMatchStats;
import es.urjc.code.backend.model.Tournament;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.MatchService;
import es.urjc.code.backend.service.TeamService;
import es.urjc.code.backend.service.TournamentService;
import es.urjc.code.backend.service.UserService;
import es.urjc.code.backend.service.MessageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.*;

@Controller
public class BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private MessageService messageService;

    // HOME
    @GetMapping("/")
    public String index(Model model) {
        // Show up to 3 active tournaments on the home page
        List<Tournament> activeTournaments = tournamentService.findActiveTournaments(3);

        model.addAttribute("activeTournaments", activeTournaments);
        model.addAttribute("totalTournaments", tournamentService.countActiveTournaments());
        model.addAttribute("totalTeams", teamService.count());
        model.addAttribute("totalUsers", userService.count());
        model.addAttribute("totalMatches", matchService.count());
        return "index";
    }

    @GetMapping("/login")
    public String login(
            Model model,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "registered", required = false) String registered) {
        if (error != null) {
            model.addAttribute("error", true);
        }
        if (registered != null) {
            model.addAttribute("registered", true);
        }
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    // PROFILE
    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        if (principal != null) {
            User user = userService.resolveUser(principal);

            if (user != null) {
                model.addAttribute("user", user);
                List<Message> messages = messageService.findByRecipientOrderBySentAtDesc(user);
                model.addAttribute("messages", messages);
                model.addAttribute("hasMessages", !messages.isEmpty());
                model.addAttribute("userIsCaptain", user.getIsCaptain());
                populateProfileStats(user, model);
            }
            return "profile";
        } else {
            return "redirect:/login";
        }
    }

    @GetMapping("/profile/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        User user = userService.findById(id).orElse(null);
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("viewingOther", true);
            populateProfileStats(user, model);
            model.addAttribute("userIsCaptain", user.getIsCaptain());
            return "profile";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("/profile/edit")
    public String editProfile(
            @RequestParam("nickname") String nickname,
            @RequestParam(value = "university", required = false) String university,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @AuthenticationPrincipal UserDetails currentUser) throws IOException {

        if (currentUser == null) {
            return "redirect:/login";
        }

        User user = userService.resolveUser(currentUser.getUsername());

        if (user != null) {
            userService.editProfile(user, nickname, university, imageFile);
        }
        return "redirect:/profile";
    }

    // FAVOURITES
    @GetMapping("/favourites")
    public String favourites(Model model,
            @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        User user = userService.resolveUser(currentUser.getUsername());

        if (user != null) {
            List<Map<String, Object>> enriched = user.getFavoriteTournaments().stream()
                    .map(t -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", t.getId());
                        m.put("name", t.getName());
                        m.put("game", t.getGame());
                        m.put("platform", t.getPlatform());
                        m.put("mode", t.getMode());
                        m.put("maxTeams", t.getMaxTeams());
                        m.put("startDate", t.getStartDate());
                        m.put("description", t.getDescription());
                        m.put("state", t.getState());
                        m.put("stateActive", "En Curso".equals(t.getState()));
                        m.put("stateUpcoming", "Próximamente".equals(t.getState()));
                        m.put("stateFinished", "Finalizado".equals(t.getState()));
                        m.put("isFavourite", true);
                        return m;
                    }).toList();
            model.addAttribute("tournaments", enriched);
        }
        return "favourites";
    }

    // ADMIN PANEL
    @GetMapping("/admin")
    public String admin(Model model,
            @RequestParam(value = "msgSent", required = false) Boolean msgSent) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("captains", userService.findCaptains());
        model.addAttribute("tournaments", tournamentService.findAll());
        model.addAttribute("teams", teamService.findAll());
        if (Boolean.TRUE.equals(msgSent)) {
            model.addAttribute("msgSent", true);
        }
        return "admin";
    }

    // ERROR 403
    @GetMapping("/error-403")
    public String error403(Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", "You do not have permission to access this page.");
        return "error";
    }

    private void populateProfileStats(User user, Model model) {
        List<PlayerMatchStats> stats = matchService.findStatsByPlayer(user);
        int totalMatches = stats.size();
        int totalKills = 0;
        int totalDeaths = 0;

        for (PlayerMatchStats s : stats) {
            totalKills += s.getKills();
            totalDeaths += s.getDeaths();
        }

        double kda = totalDeaths == 0 ? totalKills : Math.round(((double) totalKills / totalDeaths) * 100.0) / 100.0;

        int winRate = 0;
        int lossRate = 0;
        if (user.getTeam() != null && user.getTeam().getMatchesPlayed() > 0) {
            winRate = (int) Math.round(((double) user.getTeam().getWins() / user.getTeam().getMatchesPlayed()) * 100);
            lossRate = 100 - winRate;
        }

        model.addAttribute("totalMatches", totalMatches);
        model.addAttribute("kda", String.format("%.2f", kda).replace(",", "."));
        model.addAttribute("winRate", winRate);
        model.addAttribute("lossRate", lossRate);

        model.addAttribute("kdaHeight", Math.min(100, (int) (kda * 15) + 10));
        model.addAttribute("matchesHeight", Math.min(100, (totalMatches * 5) + 10));
        List<Map<String, Object>> userTournaments = new ArrayList<>();
        if (user.getTeam() != null) {
            List<Tournament> allTournaments = tournamentService.findAll();
            for (Tournament t : allTournaments) {
                if (t.getTeams().contains(user.getTeam())) {
                    Map<String, Object> tMap = new HashMap<>();
                    tMap.put("game", t.getGame());
                    tMap.put("teamName", user.getTeam().getName());
                    tMap.put("isEnCurso", "En Curso".equalsIgnoreCase(t.getState()));
                    tMap.put("isFinalizado", "Finalizado".equalsIgnoreCase(t.getState()));
                    tMap.put("isProximamente", "Próximamente".equalsIgnoreCase(t.getState()));
                    String icon = "bi-controller";
                    if (t.getGame() != null) {
                        String g = t.getGame().toLowerCase();
                        if (g.contains("valorant") || g.contains("cs"))
                            icon = "bi-mouse2";
                        else if (g.contains("league") || g.contains("dota"))
                            icon = "bi-pc-display";
                    }
                    tMap.put("gameIcon", icon);
                    userTournaments.add(tMap);
                }
            }
        }
        model.addAttribute("userTournaments", userTournaments);
        model.addAttribute("hasTournaments", !userTournaments.isEmpty());
    }
}
