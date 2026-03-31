package es.urjc.code.backend.controller;

import es.urjc.code.backend.repository.MatchRepository;
import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.TournamentRepository;
import es.urjc.code.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BaseController {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

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
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    // PROFILE
    @GetMapping("/profile")
    public String profile(Model model,
            @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        userRepository.findByEmail(currentUser.getUsername())
                .ifPresent(u -> model.addAttribute("user", u));
        return "profile";
    }

    // FAVOURITES
    @GetMapping("/favourites")
    public String favourites(Model model,
            @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        return "favourites";
    }

    // ADMIN PANEL
    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("tournaments", tournamentRepository.findAll());
        model.addAttribute("teams", teamRepository.findAll());
        return "admin";
    }

    @GetMapping("/admin/edit-tournament")
    public String editTournamentStatic() {
        return "edit-tournament";
    }

    @GetMapping("/admin/create-matches")
    public String createMatchesStatic() {
        return "create-matches";
    }

    @GetMapping("/admin/edit-matches")
    public String editMatchesStatic() {
        return "edit-matches";
    }

    @GetMapping("/admin/teams-list")
    public String teamsListAdminStatic() {
        return "teams-list-admin";
    }

    // ERROR 403 no permision)
    @GetMapping("/error-403")
    public String error403(Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", "You do not have permission to access this page.");
        return "error";
    }
}
