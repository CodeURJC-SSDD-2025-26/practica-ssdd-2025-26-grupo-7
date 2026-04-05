package es.urjc.code.backend.controller;

import es.urjc.code.backend.repository.MatchRepository;
import es.urjc.code.backend.repository.MessageRepository;
import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.TournamentRepository;
import es.urjc.code.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import es.urjc.code.backend.repository.PlayerMatchStatsRepository;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BaseController {
    @Autowired
    private PlayerMatchStatsRepository playerStatsRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MessageRepository messageRepository;

    // HOME
    @GetMapping("/")
    public String index(Model model) {
        // Show up to 3 active tournaments on the home page
        var allTournaments = tournamentRepository.findAll();
        var activeTournaments = allTournaments.stream()
                .filter(t -> !"Finalizado".equals(t.getState()))
                .limit(3)
                .toList();

        model.addAttribute("activeTournaments", activeTournaments);
        model.addAttribute("totalTournaments", allTournaments.size());
        model.addAttribute("totalTeams", teamRepository.count());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalMatches", matchRepository.count());
        return "index";
    }

    @GetMapping("/login")
    public String login(
            org.springframework.ui.Model model,
            @org.springframework.web.bind.annotation.RequestParam(value = "error", required = false) String error,
            @org.springframework.web.bind.annotation.RequestParam(value = "registered", required = false) String registered) {
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
    public String profile(Model model, java.security.Principal principal) {
        if (principal != null) {
            String identifier = principal.getName();
            es.urjc.code.backend.model.User user = userRepository.findByEmail(identifier)
                    .orElseGet(() -> userRepository.findByName(identifier).orElse(null));

            if (user != null) {
                model.addAttribute("user", user);
                // Load messages for this user (inbox)
                java.util.List<es.urjc.code.backend.model.Message> messages = messageRepository
                        .findByRecipientOrderBySentAtDesc(user);
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
    public String viewProfile(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        es.urjc.code.backend.model.User user = userRepository.findById(id).orElse(null);
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

    @org.springframework.web.bind.annotation.PostMapping("/profile/edit")
    public String editProfile(
            @org.springframework.web.bind.annotation.RequestParam("nickname") String nickname,
            @org.springframework.web.bind.annotation.RequestParam(value = "university", required = false) String university,
            @org.springframework.web.bind.annotation.RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
            @AuthenticationPrincipal UserDetails currentUser) throws java.io.IOException {

        if (currentUser == null) {
            return "redirect:/login";
        }

        es.urjc.code.backend.model.User user = userRepository.findByEmail(currentUser.getUsername())
                .orElseGet(() -> userRepository.findByName(currentUser.getUsername()).orElse(null));

        if (user != null) {
            user.setNickname(nickname);
            if (university != null) {
                user.setUniversity(university);
            }
            if (imageFile != null && !imageFile.isEmpty()) {
                user.setImageFile(
                        org.hibernate.engine.jdbc.BlobProxy.generateProxy(
                                imageFile.getInputStream(), imageFile.getSize()));
            }
            userRepository.save(user);
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
        es.urjc.code.backend.model.User user = userRepository.findByEmail(currentUser.getUsername())
                .orElseGet(() -> userRepository.findByName(currentUser.getUsername()).orElse(null));

        if (user != null) {
            java.util.List<java.util.Map<String, Object>> enriched = user.getFavoriteTournaments().stream()
                    .map(t -> {
                        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
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
                        m.put("isFavourite", true); // They are all favourites on this page
                        return m;
                    }).toList();
            model.addAttribute("tournaments", enriched);
        }
        return "favourites";
    }

    // ADMIN PANEL
    @GetMapping("/admin")
    public String admin(Model model,
            @org.springframework.web.bind.annotation.RequestParam(value = "msgSent", required = false) Boolean msgSent) {
        java.util.List<es.urjc.code.backend.model.User> allUsers = userRepository.findAll();
        java.util.List<es.urjc.code.backend.model.User> captains = allUsers.stream()
                .filter(es.urjc.code.backend.model.User::getIsCaptain)
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("users", allUsers);
        model.addAttribute("captains", captains);
        model.addAttribute("tournaments", tournamentRepository.findAll());
        model.addAttribute("teams", teamRepository.findAll());
        if (Boolean.TRUE.equals(msgSent)) {
            model.addAttribute("msgSent", true);
        }
        return "admin";
    }

    // ERROR 403 no permision)
    @GetMapping("/error-403")
    public String error403(Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", "You do not have permission to access this page.");
        return "error";
    }

    private void populateProfileStats(es.urjc.code.backend.model.User user, Model model) {
        java.util.List<es.urjc.code.backend.model.PlayerMatchStats> stats = playerStatsRepository.findByPlayer(user);
        int totalMatches = stats.size();
        int totalKills = 0;
        int totalDeaths = 0;

        for (es.urjc.code.backend.model.PlayerMatchStats s : stats) {
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
        java.util.List<java.util.Map<String, Object>> userTournaments = new java.util.ArrayList<>();
        if (user.getTeam() != null) {
            java.util.List<es.urjc.code.backend.model.Tournament> allTournaments = tournamentRepository.findAll();
            for (es.urjc.code.backend.model.Tournament t : allTournaments) {
                if (t.getTeams().contains(user.getTeam())) {
                    java.util.Map<String, Object> tMap = new java.util.HashMap<>();
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
