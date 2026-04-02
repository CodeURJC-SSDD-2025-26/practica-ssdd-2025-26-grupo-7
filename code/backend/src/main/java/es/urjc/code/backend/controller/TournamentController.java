package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.Tournament;
import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.TournamentRepository;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Controller
public class TournamentController {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private es.urjc.code.backend.repository.UserRepository userRepository;

    private es.urjc.code.backend.model.User resolveUser(Principal principal) {
        if (principal == null)
            return null;
        return userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByName(principal.getName()))
                .orElse(null);
    }

    private Map<String, Object> enrichTournament(Tournament t, es.urjc.code.backend.model.User currentUser) {
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
        m.put("isFavourite", currentUser != null && currentUser.getFavoriteTournaments().contains(t));
        return m;
    }

    @GetMapping("/tournaments")
    public String getTournaments(
            Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String state,
            Principal principal) {

        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String gameParam = (game != null && !game.isBlank()) ? game : null;
        String stateParam = (state != null && !state.isBlank()) ? state : null;

        List<Tournament> tournaments = tournamentRepository.findWithFilters(searchParam, gameParam, stateParam);

        es.urjc.code.backend.model.User currentUser = resolveUser(principal);

        List<Map<String, Object>> tournamentViews = tournaments.stream()
                .map(t -> enrichTournament(t, currentUser))
                .collect(Collectors.toList());

        model.addAttribute("tournaments", tournamentViews);
        model.addAttribute("noResults", tournamentViews.isEmpty());
        model.addAttribute("hasFilters", searchParam != null || gameParam != null || stateParam != null);
        model.addAttribute("filterSearch", search != null ? search : "");
        model.addAttribute("filterGame", game != null ? game : "");
        model.addAttribute("filterState", state != null ? state : "");
        return "tournaments";
    }

    @GetMapping("/tournaments/{id}")
    public String tournamentDetail(@PathVariable Long id, Model model) {
        Optional<Tournament> opt = tournamentRepository.findById(id);
        if (opt.isEmpty())
            return "redirect:/tournaments";

        Tournament tournament = opt.get();
        AtomicInteger rank = new AtomicInteger(1);

        List<Map<String, Object>> rankedTeams = tournament.getTeams().stream()
                .sorted(Comparator.comparingInt(Team::getWins).reversed())
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("rank", rank.getAndIncrement());
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("matchesPlayed", t.getMatchesPlayed());
                    m.put("wins", t.getWins());
                    m.put("losses", t.getLosses());
                    m.put("points", t.getWins() * 3);
                    return m;
                })
                .toList();

        model.addAttribute("tournament", tournament);
        model.addAttribute("teams", rankedTeams);
        model.addAttribute("matches", tournament.getMatches());
        model.addAttribute("teamsCount", tournament.getTeams().size());
        model.addAttribute("allTeams", teamRepository.findAll());
        return "tournament-detail";
    }

    @GetMapping("/tournaments/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createTournamentForm(Model model) {
        model.addAttribute("allTeams", teamRepository.findAll());
        return "create-tournament";
    }

    @PostMapping("/tournaments/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createTournament(
            @RequestParam String name,
            @RequestParam String game,
            @RequestParam String platform,
            @RequestParam String mode,
            @RequestParam int maxTeams,
            @RequestParam String startDate,
            @RequestParam String description,
            @RequestParam String rules,
            @RequestParam(required = false) MultipartFile imageFile) throws IOException {

        Tournament t = new Tournament(name, game, platform, mode, maxTeams, startDate, description, rules);
        if (imageFile != null && !imageFile.isEmpty()) {
            t.setImageFile(BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }
        tournamentRepository.save(t);
        return "redirect:/tournaments";
    }

    @GetMapping("/tournaments/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editTournamentForm(@PathVariable Long id, Model model) {
        return tournamentRepository.findById(id).map(t -> {
            model.addAttribute("tournament", t);
            model.addAttribute("allTeams", teamRepository.findAll());
            return "edit-tournament";
        }).orElse("redirect:/tournaments");
    }

    @PostMapping("/tournaments/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editTournament(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String game,
            @RequestParam String platform,
            @RequestParam String mode,
            @RequestParam int maxTeams,
            @RequestParam String startDate,
            @RequestParam String description,
            @RequestParam String rules,
            @RequestParam String state,
            @RequestParam(required = false) MultipartFile imageFile) throws IOException {

        Optional<Tournament> opt = tournamentRepository.findById(id);
        if (opt.isEmpty())
            return "redirect:/tournaments";

        Tournament t = opt.get();
        t.setName(name);
        t.setGame(game);
        t.setPlatform(platform);
        t.setMode(mode);
        t.setMaxTeams(maxTeams);
        t.setStartDate(startDate);
        t.setDescription(description);
        t.setRules(rules);
        t.setState(state);
        if (imageFile != null && !imageFile.isEmpty()) {
            t.setImageFile(BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }
        tournamentRepository.save(t);
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/tournaments/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteTournament(@PathVariable Long id) {
        tournamentRepository.deleteById(id);
        return "redirect:/tournaments";
    }

    @PostMapping("/tournaments/{id}/teams/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addTeam(@PathVariable Long id, @RequestParam Long teamId) {
        tournamentRepository.findById(id).ifPresent(t -> teamRepository.findById(teamId).ifPresent(team -> {
            if (!t.getTeams().contains(team)) {
                t.getTeams().add(team);
                tournamentRepository.save(t);
            }
        }));
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/tournaments/{id}/teams/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public String removeTeam(@PathVariable Long id, @RequestParam Long teamId) {
        tournamentRepository.findById(id).ifPresent(t -> teamRepository.findById(teamId).ifPresent(team -> {
            t.getTeams().remove(team);
            tournamentRepository.save(t);
        }));
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/tournaments/{id}/toggle-favorite")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> toggleFavorite(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        Optional<es.urjc.code.backend.model.User> optUser = userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByName(principal.getName()));
        Optional<Tournament> optTour = tournamentRepository.findById(id);

        if (optUser.isPresent() && optTour.isPresent()) {
            es.urjc.code.backend.model.User user = optUser.get();
            Tournament t = optTour.get();
            if (user.getFavoriteTournaments().contains(t)) {
                user.getFavoriteTournaments().remove(t);
            } else {
                user.getFavoriteTournaments().add(t);
            }
            userRepository.save(user);
            return org.springframework.http.ResponseEntity.ok().build();
        }
        return org.springframework.http.ResponseEntity.badRequest().build();
    }
}