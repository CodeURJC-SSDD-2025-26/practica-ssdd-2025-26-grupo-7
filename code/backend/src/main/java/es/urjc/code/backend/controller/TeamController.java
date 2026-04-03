package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.UserRepository;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
public class TeamController {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    // LIST
    @GetMapping("/teams")
    public String teams(Model model) {
        List<Team> teams = teamRepository.findAll();
        model.addAttribute("teams", teams);
        return "teams-list";
    }

    // DETAIL
    @GetMapping("/teams/{id}")
    public String teamDetail(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails currentUser) {
        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/teams";
        }
        Team team = opt.get();
        model.addAttribute("team", team);
        model.addAttribute("players", team.getPlayers());

        int played = team.getMatchesPlayed();
        int winRate = played > 0 ? (int) Math.round((team.getWins() * 100.0) / played) : 0;
        model.addAttribute("winRate", winRate);

        if (currentUser != null) {
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isCaptain = team.getCaptain() != null
                    && team.getCaptain().getEmail().equals(currentUser.getUsername());
            model.addAttribute("canEdit", isAdmin || isCaptain);
            model.addAttribute("isAdmin", isAdmin);
        } else {
            model.addAttribute("canEdit", false);
            model.addAttribute("isAdmin", false);
        }

        return "teams";
    }

    // CREATE
    @GetMapping("/teams/create")
    @PreAuthorize("isAuthenticated()")
    public String createTeamForm() {
        return "create-team";
    }

    @PostMapping("/teams/create")
    @PreAuthorize("isAuthenticated()")
    public String createTeam(
            @RequestParam String name,
            @RequestParam String university,
            @RequestParam String mainGame,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile imageFile,
            @AuthenticationPrincipal UserDetails currentUser) throws IOException {

        Team team = new Team(name, university, mainGame, description);

        if (imageFile != null && !imageFile.isEmpty()) {
            team.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }

        Optional<User> userOpt = userRepository.findByEmail(currentUser.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            team.setCaptain(user);
            team.getPlayers().add(user);
            teamRepository.save(team);
            user.setTeam(team);
            userRepository.save(user);
        } else {
            teamRepository.save(team);
        }

        return "redirect:/teams";
    }

    // EDIT
    @GetMapping("/teams/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editTeamForm(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails currentUser) {
        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/teams";
        }
        Team team = opt.get();

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isCaptain = team.getCaptain() != null
                && team.getCaptain().getEmail().equals(currentUser.getUsername());

        if (!isAdmin && !isCaptain) {
            return "redirect:/error-403";
        }

        model.addAttribute("team", team);
        model.addAttribute("teamId", id);
        model.addAttribute("allUsers", userRepository.findAll());
        model.addAttribute("availableUsers", userRepository.findByTeamIsNull());
        
        // Pre-calculate roster with isCaptain flag to avoid LazyLoading/Scoping issues in Mustache
        java.util.List<java.util.Map<String, Object>> playersList = new java.util.ArrayList<>();
        User captain = team.getCaptain();
        for (User player : team.getPlayers()) {
            java.util.Map<String, Object> pData = new java.util.HashMap<>();
            pData.put("id", player.getId());
            pData.put("nickname", player.getNickname());
            pData.put("isCaptain", captain != null && captain.getId().equals(player.getId()));
            playersList.add(pData);
        }
        model.addAttribute("playersList", playersList);
        model.addAttribute("totalPlayers", playersList.size());
        
        return "edit-team";
    }

    @PostMapping("/teams/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editTeam(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String university,
            @RequestParam String mainGame,
            @RequestParam String description,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long captainId,
            @RequestParam(required = false) MultipartFile imageFile,
            @AuthenticationPrincipal UserDetails currentUser) throws IOException {

        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/teams";
        }
        Team team = opt.get();

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isCaptain = team.getCaptain() != null
                && team.getCaptain().getEmail().equals(currentUser.getUsername());

        if (!isAdmin && !isCaptain) {
            return "redirect:/error-403";
        }

        team.setName(name);
        team.setUniversity(university);
        team.setMainGame(mainGame);
        team.setDescription(description);
        team.setTag(tag);

        if (captainId != null) {
            userRepository.findById(captainId).ifPresent(newCaptain -> {
                team.setCaptain(newCaptain);
                // Ensure the captain is also a member of the team
                if (!team.getPlayers().contains(newCaptain)) {
                    // Unlink from previous team if any
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
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            team.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }

        teamRepository.save(team);
        return "redirect:/teams/" + id;
    }

    @PostMapping("/teams/{id}/add-player")
    @PreAuthorize("isAuthenticated()")
    public String addPlayer(@PathVariable Long id, @RequestParam Long userId,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isPresent()) {
            Team team = opt.get();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isCaptain = team.getCaptain() != null
                    && team.getCaptain().getEmail().equals(currentUser.getUsername());

            if (isAdmin || isCaptain) {
                userRepository.findById(userId).ifPresent(user -> {
                    if (user.getTeam() == null) {
                        user.setTeam(team);
                        userRepository.save(user);
                        team.getPlayers().add(user);
                        teamRepository.save(team);
                    }
                });
            }
        }
        return "redirect:/teams/" + id + "/edit";
    }

    @PostMapping("/teams/{id}/remove-player")
    @PreAuthorize("isAuthenticated()")
    public String removePlayer(@PathVariable Long id, @RequestParam Long userId,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isPresent()) {
            Team team = opt.get();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isCaptain = team.getCaptain() != null
                    && team.getCaptain().getEmail().equals(currentUser.getUsername());

            if (isAdmin || isCaptain) {
                userRepository.findById(userId).ifPresent(user -> {
                    if (team.getPlayers().contains(user)) {
                        user.setTeam(null);
                        userRepository.save(user);
                        team.getPlayers().remove(user);
                        // If the removed player was captain, unset captain
                        if (team.getCaptain() != null && team.getCaptain().equals(user)) {
                            team.setCaptain(null);
                        }
                        teamRepository.save(team);
                    }
                });
            }
        }
        return "redirect:/teams/" + id + "/edit";
    }

    // DELETE
    @PostMapping("/teams/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteTeam(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isPresent()) {
            Team team = opt.get();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isCaptain = team.getCaptain() != null
                    && team.getCaptain().getEmail().equals(currentUser.getUsername());

            if (isAdmin || isCaptain) {
                teamRepository.deleteById(id);
            }
        }
        return "redirect:/teams";
    }

    @PostMapping("/admin/teams/{id}/delete")
    public String adminDeleteTeam(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        if (currentUser == null) return "redirect:/login";

        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isPresent()) {
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {
                // Unlink players first
                Team team = opt.get();
                for (User player : team.getPlayers()) {
                    player.setTeam(null);
                    userRepository.save(player);
                }
                teamRepository.deleteById(id);
            }
        }
        return "redirect:/admin/teams-list";
    }
}
