package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.TeamService;
import es.urjc.code.backend.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Controller
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    // LIST
    @GetMapping("/teams")
    public String teams(Model model) {
        model.addAttribute("teams", teamService.findAll());
        return "teams-list";
    }

    // DETAIL
    @GetMapping("/teams/{id}")
    public String teamDetail(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails currentUser) {

        Team team = teamService.loadTeamWithStats(id);
        if (team == null) {
            return "redirect:/teams";
        }

        List<Match> teamMatches = teamService.findTeamMatches(team);
        int played = team.getMatchesPlayed();
        int winRate = played > 0 ? (int) Math.round((team.getWins() * 100.0) / played) : 0;

        model.addAttribute("team", team);
        model.addAttribute("players", team.getPlayers());
        model.addAttribute("matches", teamMatches);
        model.addAttribute("winRate", winRate);

        if (currentUser != null) {
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isCaptain = teamService.isCaptainOf(team, currentUser.getUsername());
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
    public String createTeamForm(Model model) {
        model.addAttribute("availableUsers", userService.findAvailableUsers());
        return "create-team";
    }

    @PostMapping("/teams/create")
    @PreAuthorize("isAuthenticated()")
    public String createTeam(
            @RequestParam String name,
            @RequestParam String university,
            @RequestParam String mainGame,
            @RequestParam String description,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long captainId,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) MultipartFile bannerFile,
            @AuthenticationPrincipal UserDetails currentUser) throws IOException {

        String username = currentUser != null ? currentUser.getUsername() : null;
        teamService.createTeam(name, university, mainGame, description, tag,
                captainId, imageFile, bannerFile, username);

        return "redirect:/teams";
    }

    // EDIT
    @GetMapping("/teams/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editTeamForm(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails currentUser) {
        Optional<Team> opt = teamService.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/teams";
        }
        Team team = opt.get();

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isCaptain = teamService.isCaptainOf(team, currentUser.getUsername());

        if (!isAdmin && !isCaptain) {
            return "redirect:/error-403";
        }

        model.addAttribute("team", team);
        model.addAttribute("teamId", id);
        model.addAttribute("availableUsers", userService.findAvailableUsers());

        List<Map<String, Object>> playersList = new ArrayList<>();
        User captain = team.getCaptain();
        for (User player : team.getPlayers()) {
            Map<String, Object> pData = new HashMap<>();
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
            @RequestParam(required = false) MultipartFile bannerFile,
            @AuthenticationPrincipal UserDetails currentUser) throws IOException {

        Optional<Team> opt = teamService.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/teams";
        }
        Team team = opt.get();

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isCaptain = teamService.isCaptainOf(team, currentUser.getUsername());

        if (!isAdmin && !isCaptain) {
            return "redirect:/error-403";
        }

        teamService.editTeam(id, name, university, mainGame, description, tag,
                captainId, imageFile, bannerFile);
        return "redirect:/teams/" + id;
    }

    @PostMapping("/teams/{id}/add-player")
    @PreAuthorize("isAuthenticated()")
    public String addPlayer(@PathVariable Long id, @RequestParam Long userId,
            @AuthenticationPrincipal UserDetails currentUser) {

        Optional<Team> opt = teamService.findById(id);
        if (opt.isPresent()) {
            Team team = opt.get();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isCaptain = teamService.isCaptainOf(team, currentUser.getUsername());

            if (isAdmin || isCaptain) {
                teamService.addPlayer(id, userId);
            }
        }
        return "redirect:/teams/" + id + "/edit";
    }

    @PostMapping("/teams/{id}/remove-player")
    @PreAuthorize("isAuthenticated()")
    public String removePlayer(@PathVariable Long id, @RequestParam Long userId,
            @AuthenticationPrincipal UserDetails currentUser) {

        Optional<Team> opt = teamService.findById(id);
        if (opt.isPresent()) {
            Team team = opt.get();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isCaptain = teamService.isCaptainOf(team, currentUser.getUsername());

            if (isAdmin || isCaptain) {
                teamService.removePlayer(id, userId);
            }
        }
        return "redirect:/teams/" + id + "/edit";
    }

    @GetMapping("/admin/teams-list")
    public String adminTeamsList(Model model) {
        model.addAttribute("teams", teamService.prepareAdminTeamsList());
        model.addAttribute("totalTeams", teamService.findAll().size());
        return "teams-list-admin";
    }

    // DELETE
    @PostMapping("/teams/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteTeam(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        Optional<Team> opt = teamService.findById(id);
        if (opt.isPresent()) {
            Team team = opt.get();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isCaptain = teamService.isCaptainOf(team, currentUser.getUsername());

            if (isAdmin || isCaptain) {
                teamService.deleteTeam(team);
            }
        }
        return "redirect:/teams";
    }

    @PostMapping("/admin/teams/{id}/delete")
    public String adminDeleteTeam(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        if (currentUser == null)
            return "redirect:/login";

        Optional<Team> opt = teamService.findById(id);
        if (opt.isPresent()) {
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {
                teamService.deleteTeam(opt.get());
            }
        }
        return "redirect:/admin/teams-list";
    }

    @GetMapping("/api/teams/{id}/players")
    @ResponseBody
    public List<Map<String, Object>> getTeamPlayers(@PathVariable Long id) {
        return teamService.getTeamPlayersForApi(id);
    }
}
