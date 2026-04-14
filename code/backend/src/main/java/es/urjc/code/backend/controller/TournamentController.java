package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Match;
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
        m.put("rules", t.getRules());
        m.put("state", t.getState());
        m.put("stateActive", "En Curso".equals(t.getState()));
        m.put("stateUpcoming", "Próximamente".equals(t.getState()));
        m.put("stateFinished", "Finalizado".equals(t.getState()));
        m.put("isFavourite", currentUser != null && currentUser.getFavoriteTournaments().contains(t));
        return m;
    }

    private Map<String, Object> enrichMatch(Match match) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", match.getId());
        m.put("matchDate", match.getMatchDate());
        m.put("phase", match.getPhase());
        m.put("format", match.getFormat());
        m.put("state", match.getState());
        m.put("scoreLocal", match.getScoreLocal());
        m.put("scoreAway", match.getScoreAway());
        m.put("result", match.getResult());
        m.put("notes", match.getNotes());

        String state = match.getState() != null ? match.getState() : "";
        m.put("matchLive", "En Vivo".equals(state));
        m.put("matchFinished", "Finalizado".equals(state));
        m.put("matchScheduled", "Programado".equals(state));
        m.put("matchCancelled", "Cancelado".equals(state));

        if (match.getLocalTeam() != null) {
            Team lt = match.getLocalTeam();
            Map<String, Object> ltMap = new LinkedHashMap<>();
            ltMap.put("id", lt.getId());
            ltMap.put("name", lt.getName());
            m.put("localTeam", ltMap);
        }

        if (match.getAwayTeam() != null) {
            Team at = match.getAwayTeam();
            Map<String, Object> atMap = new LinkedHashMap<>();
            atMap.put("id", at.getId());
            atMap.put("name", at.getName());
            m.put("awayTeam", atMap);
        }

        return m;
    }

    @GetMapping("/tournaments")
    public String getTournaments(
            Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            Principal principal) {

        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String gameParam = (game != null && !game.isBlank()) ? game : null;
        String stateParam = (state != null && !state.isBlank()) ? state : null;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 3);
        org.springframework.data.domain.Page<Tournament> tournamentPage = tournamentRepository.findWithFilters(searchParam, gameParam, stateParam, pageable);

        es.urjc.code.backend.model.User currentUser = resolveUser(principal);

        List<Map<String, Object>> tournamentViews = tournamentPage.getContent().stream()
                .map(t -> enrichTournament(t, currentUser))
                .collect(Collectors.toList());

        model.addAttribute("tournaments", tournamentViews);
        model.addAttribute("noResults", tournamentViews.isEmpty());
        model.addAttribute("hasFilters", searchParam != null || gameParam != null || stateParam != null);
        model.addAttribute("filterSearch", search != null ? search : "");
        model.addAttribute("filterGame", game != null ? game : "");
        model.addAttribute("filterState", state != null ? state : "");

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tournamentPage.getTotalPages());
        model.addAttribute("hasPages", tournamentPage.getTotalPages() > 0);
        model.addAttribute("hasPrevious", tournamentPage.hasPrevious());
        model.addAttribute("hasNext", tournamentPage.hasNext());
        model.addAttribute("previousPage", page - 1);
        model.addAttribute("nextPage", page + 1);

        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = 0; i < tournamentPage.getTotalPages(); i++) {
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("number", i);
            pageData.put("displayNumber", i + 1);
            pageData.put("isCurrent", i == page);
            
            // Build query params string to maintain filters when paginating
            StringBuilder query = new StringBuilder();
            if (search != null && !search.isBlank()) query.append("&search=").append(search);
            if (game != null && !game.isBlank()) query.append("&game=").append(game);
            if (state != null && !state.isBlank()) query.append("&state=").append(state);
            pageData.put("query", query.toString());
            
            pages.add(pageData);
        }
        model.addAttribute("pages", pages);

        // Save current query for Prev/Next buttons
        StringBuilder currentQuery = new StringBuilder();
        if (search != null && !search.isBlank()) currentQuery.append("&search=").append(search);
        if (game != null && !game.isBlank()) currentQuery.append("&game=").append(game);
        if (state != null && !state.isBlank()) currentQuery.append("&state=").append(state);
        model.addAttribute("currentQuery", currentQuery.toString());

        return "tournaments";
    }

    @GetMapping("/tournaments/{id}")
    public String tournamentDetail(@PathVariable Long id, Model model) {
        Optional<Tournament> opt = tournamentRepository.findById(id);
        if (opt.isEmpty())
            return "redirect:/tournaments";

        Tournament tournament = opt.get();

        String tState = tournament.getState() != null ? tournament.getState() : "";
        model.addAttribute("stateActive", "En Curso".equals(tState));
        model.addAttribute("stateUpcoming", "Próximamente".equals(tState));
        model.addAttribute("stateFinished", "Finalizado".equals(tState));

        List<Match> tournamentMatches = tournament.getMatches();

        class TeamStats {
            int wins = 0;
            int losses = 0;
            int played = 0;
        }

        Map<Long, TeamStats> statsMap = new HashMap<>();
        for (Team t : tournament.getTeams()) {
            statsMap.put(t.getId(), new TeamStats());
        }

        for (Match m : tournamentMatches) {
            if ("Finalizado".equals(m.getState())) {
                Team local = m.getLocalTeam();
                Team away = m.getAwayTeam();

                if (local != null && statsMap.containsKey(local.getId())) {
                    TeamStats s = statsMap.get(local.getId());
                    s.played++;
                    if (m.getScoreLocal() > m.getScoreAway())
                        s.wins++;
                    else if (m.getScoreLocal() < m.getScoreAway())
                        s.losses++;
                }
                if (away != null && statsMap.containsKey(away.getId())) {
                    TeamStats s = statsMap.get(away.getId());
                    s.played++;
                    if (m.getScoreAway() > m.getScoreLocal())
                        s.wins++;
                    else if (m.getScoreAway() < m.getScoreLocal())
                        s.losses++;
                }
            }
        }

        AtomicInteger rank = new AtomicInteger(1);
        List<Map<String, Object>> rankedTeams = tournament.getTeams().stream()
                .map(t -> {
                    TeamStats s = statsMap.get(t.getId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("wins", s.wins);
                    m.put("losses", s.losses);
                    m.put("matchesPlayed", s.played);
                    m.put("points", s.wins * 3);
                    return m;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("points"), (int) a.get("points")))
                .map(m -> {
                    int r = rank.getAndIncrement();
                    m.put("rank", r);
                    m.put("rankIs1", r == 1);
                    m.put("rankIs2", r == 2);
                    m.put("rankIs3", r == 3);
                    m.put("rankGt3", r > 3);
                    return m;
                })
                .toList();

        List<Map<String, Object>> enrichedMatches = tournament.getMatches().stream()
                .map(this::enrichMatch)
                .collect(Collectors.toList());

        model.addAttribute("tournament", tournament);
        model.addAttribute("teams", rankedTeams);
        model.addAttribute("matches", enrichedMatches);
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
        return "redirect:/admin/tournaments";
    }

    @GetMapping("/admin/tournaments")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminTournaments(Model model) {
        model.addAttribute("tournaments", tournamentRepository.findAll());
        return "tournaments-list-admin";
    }

    @GetMapping("/admin/tournaments/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editTournamentSelection(Model model) {
        model.addAttribute("allTournaments", tournamentRepository.findAll());
        return "edit-tournament";
    }

    @GetMapping("/admin/tournaments/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editTournamentForm(@PathVariable Long id, Model model, Principal principal) {
        return tournamentRepository.findById(id).map(t -> {
            es.urjc.code.backend.model.User currentUser = resolveUser(principal);
            model.addAttribute("tournament", enrichTournament(t, currentUser));
            model.addAttribute("allTeams", teamRepository.findAll());
            model.addAttribute("teams", t.getTeams());

            List<Map<String, Object>> enrichedMatches = t.getMatches().stream()
                    .map(this::enrichMatch)
                    .collect(Collectors.toList());
            model.addAttribute("matches", enrichedMatches);

            return "edit-tournament";
        }).orElse("redirect:/admin/tournaments/edit");
    }

    @PostMapping("/admin/tournaments/edit/{id}")
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
            return "redirect:/admin/tournaments/edit";

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
        return "redirect:/admin/tournaments/edit/" + id;
    }

    @PostMapping("/tournaments/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteTournament(@PathVariable Long id) {
        tournamentRepository.deleteById(id);
        return "redirect:/tournaments";
    }

    @PostMapping("/admin/tournaments/edit/{id}/teams/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addTeam(@PathVariable Long id, @RequestParam Long teamId) {
        tournamentRepository.findById(id).ifPresent(t -> teamRepository.findById(teamId).ifPresent(team -> {
            if (!t.getTeams().contains(team)) {
                t.getTeams().add(team);
                tournamentRepository.save(t);
            }
        }));
        return "redirect:/admin/tournaments/edit/" + id;
    }

    @PostMapping("/admin/tournaments/edit/{id}/teams/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public String removeTeam(@PathVariable Long id, @RequestParam Long teamId) {
        tournamentRepository.findById(id).ifPresent(t -> teamRepository.findById(teamId).ifPresent(team -> {
            t.getTeams().remove(team);
            tournamentRepository.save(t);
        }));
        return "redirect:/admin/tournaments/edit/" + id;
    }

    @Autowired
    private es.urjc.code.backend.service.PdfService pdfService;

    @GetMapping("/tournaments/{id}/pdf")
    public org.springframework.http.ResponseEntity<byte[]> downloadTournamentPdf(@PathVariable Long id) {
        Optional<Tournament> opt = tournamentRepository.findById(id);
        if (opt.isEmpty()) return org.springframework.http.ResponseEntity.notFound().build();
        
        Tournament t = opt.get();
        byte[] pdf = pdfService.generateTournamentPdf(t);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Tournament_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new org.springframework.http.ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
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