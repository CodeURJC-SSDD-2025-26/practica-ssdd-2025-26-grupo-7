package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.Tournament;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.MatchRepository;
import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.TournamentRepository;
import es.urjc.code.backend.repository.UserRepository;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    // LIST
    @GetMapping("/teams")
    public String teams(Model model) {
        List<Team> teams = teamRepository.findAll();
        model.addAttribute("teams", teams);
        return "teams-list";
    }

    // DETAIL
    @Transactional
    @GetMapping("/teams/{id}")
    public String teamDetail(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails currentUser) {
        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/teams";
        }
        Team team = opt.get();
        List<Match> teamMatches = matchRepository.findByLocalTeamOrAwayTeam(team, team);
        int wins = 0;
        int losses = 0;
        int played = 0;

        for (Match m : teamMatches) {
            if ("Finalizado".equals(m.getState())) {
                played++;
                boolean isLocal = m.getLocalTeam().getId().equals(id);
                int scoreM = isLocal ? m.getScoreLocal() : m.getScoreAway();
                int scoreO = isLocal ? m.getScoreAway() : m.getScoreLocal();
                if (scoreM > scoreO) wins++;
                else if (scoreM < scoreO) losses++;
            }
        }
        
        team.setWins(wins);
        team.setLosses(losses);
        team.setMatchesPlayed(played);
        teamRepository.saveAndFlush(team);

        model.addAttribute("team", team);
        model.addAttribute("players", team.getPlayers());
        model.addAttribute("matches", teamMatches);

        int winRate = played > 0 ? (int) Math.round((wins * 100.0) / played) : 0;
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
    public String createTeamForm(Model model) {
        model.addAttribute("availableUsers", userRepository.findByTeamIsNullAndEnabledTrue());
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

        Team team = new Team(name, university, mainGame, description);
        if (tag != null)
            team.setTag(tag);

        if (imageFile != null && !imageFile.isEmpty()) {
            team.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            team.setBannerFile(
                    BlobProxy.generateProxy(bannerFile.getInputStream(), bannerFile.getSize()));
        }

        User captain = null;
        if (captainId != null) {
            captain = userRepository.findById(captainId).orElse(null);
        }

        if (captain == null && currentUser != null) {
            captain = userRepository.findByEmail(currentUser.getUsername()).orElse(null);
        }

        if (captain != null) {
            team.setCaptain(captain);
            team.getPlayers().add(captain);
            teamRepository.save(team);
            captain.setTeam(team);
            userRepository.save(captain);
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
        model.addAttribute("availableUsers", userRepository.findByTeamIsNullAndEnabledTrue());

        // Pre-calculate roster with isCaptain flag to avoid LazyLoading/Scoping issues
        // in Mustache
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
            @RequestParam(required = false) MultipartFile bannerFile,
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
        } else {
            team.setCaptain(null);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            team.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            team.setBannerFile(
                    BlobProxy.generateProxy(bannerFile.getInputStream(), bannerFile.getSize()));
        }

        teamRepository.save(team);
        return "redirect:/teams/" + id;
    }

    @Transactional
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
                        team.getPlayers().add(user);

                        userRepository.save(user);
                        teamRepository.save(team);
                    }
                });
            }
        }
        return "redirect:/teams/" + id + "/edit";
    }

    @Transactional
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
                    // Remove player from the list using ID comparison
                    team.getPlayers().removeIf(p -> p.getId().equals(userId));

                    // Unset captain if this player was the captain
                    if (team.getCaptain() != null && team.getCaptain().getId().equals(userId)) {
                        team.setCaptain(null);
                    }

                    // Unlink user from team
                    user.setTeam(null);

                    userRepository.save(user);
                    teamRepository.save(team);
                });
            }
        }
        return "redirect:/teams/" + id + "/edit";
    }

    @GetMapping("/admin/teams-list")
    public String adminTeamsList(Model model) {
        List<Team> teams = teamRepository.findAll();

        // Prepare teams with stats specifically for the template
        List<java.util.Map<String, Object>> preparedTeams = teams.stream().map(t -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("university", t.getUniversity());
            map.put("mainGame", t.getMainGame());
            map.put("wins", t.getWins());
            map.put("losses", t.getLosses());
            map.put("points", t.getWins() * 3);

            int played = t.getMatchesPlayed();
            int winRate = played > 0 ? (int) Math.round((t.getWins() * 100.0) / played) : 0;
            map.put("winRate", winRate);

            return map;
        }).toList();

        model.addAttribute("teams", preparedTeams);
        model.addAttribute("totalTeams", teams.size());
        return "teams-list-admin";
    }

    // DELETE
    @Transactional
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
                performFullTeamDeletion(team);
            }
        }
        return "redirect:/teams";
    }

    @Transactional
    @PostMapping("/admin/teams/{id}/delete")
    public String adminDeleteTeam(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        if (currentUser == null)
            return "redirect:/login";

        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isPresent()) {
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {
                performFullTeamDeletion(opt.get());
            }
        }
        return "redirect:/admin/teams-list";
    }

    @GetMapping("/api/teams/{id}/players")
    @ResponseBody
    public List<java.util.Map<String, Object>> getTeamPlayers(@PathVariable Long id) {
        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isPresent()) {
            return opt.get().getPlayers().stream().map(p -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", p.getId());
                map.put("nickname", p.getNickname());
                return map;
            }).toList();
        }
        return java.util.Collections.emptyList();
    }

    private void performFullTeamDeletion(Team team) {
        java.util.List<User> players = new java.util.ArrayList<>(team.getPlayers());
        for (User player : players) {
            player.setTeam(null);
            userRepository.save(player);
        }
        team.getPlayers().clear();
        teamRepository.saveAndFlush(team);

        java.util.List<es.urjc.code.backend.model.Match> matches = matchRepository.findByLocalTeamOrAwayTeam(team,
                team);
        for (es.urjc.code.backend.model.Match match : matches) {
            Tournament t = match.getTournament();
            if (t != null) {
                t.getMatches().remove(match);
                tournamentRepository.save(t);
            }
        }
        matchRepository.deleteAll(matches);
        matchRepository.flush();

        java.util.List<Tournament> tournaments = tournamentRepository.findByTeamsContaining(team);
        for (Tournament tournament : tournaments) {
            tournament.getTeams().remove(team);
            tournamentRepository.save(tournament);
        }
        tournamentRepository.flush();

        teamRepository.delete(team);
        teamRepository.flush();
    }
}
