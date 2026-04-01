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
                java.util.List<es.urjc.code.backend.model.Message> messages =
                        messageRepository.findByRecipientOrderBySentAtDesc(user);
                model.addAttribute("messages", messages);
                model.addAttribute("hasMessages", !messages.isEmpty());
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

    @GetMapping("/matches")
    public String showMatchesPage() {
        return "matches";
    }

    @GetMapping("/match-detail")
    public String showMatchesDetailPage() {
        return "match-detail";
    }

    // ERROR 403 no permision)
    @GetMapping("/error-403")
    public String error403(Model model) {
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", "You do not have permission to access this page.");
        return "error";
    }
}
