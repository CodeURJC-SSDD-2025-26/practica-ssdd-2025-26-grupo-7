package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Tournament;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.TeamService;
import es.urjc.code.backend.service.TournamentService;
import es.urjc.code.backend.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

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
        org.springframework.data.domain.Page<Tournament> tournamentPage = tournamentService.findWithFilters(searchParam,
                gameParam, stateParam, pageable);

        User currentUser = userService.resolveUser(principal);

        List<Map<String, Object>> tournamentViews = tournamentPage.getContent().stream()
                .map(t -> tournamentService.enrichTournament(t, currentUser))
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

            StringBuilder query = new StringBuilder();
            if (search != null && !search.isBlank())
                query.append("&search=").append(search);
            if (game != null && !game.isBlank())
                query.append("&game=").append(game);
            if (state != null && !state.isBlank())
                query.append("&state=").append(state);
            pageData.put("query", query.toString());

            pages.add(pageData);
        }
        model.addAttribute("pages", pages);

        StringBuilder currentQuery = new StringBuilder();
        if (search != null && !search.isBlank())
            currentQuery.append("&search=").append(search);
        if (game != null && !game.isBlank())
            currentQuery.append("&game=").append(game);
        if (state != null && !state.isBlank())
            currentQuery.append("&state=").append(state);
        model.addAttribute("currentQuery", currentQuery.toString());

        return "tournaments";
    }

    @GetMapping("/tournaments/{id}")
    public String tournamentDetail(@PathVariable Long id, Model model) {
        Optional<Tournament> opt = tournamentService.findById(id);
        if (opt.isEmpty())
            return "redirect:/tournaments";

        Tournament tournament = opt.get();

        String tState = tournament.getState() != null ? tournament.getState() : "";
        model.addAttribute("stateActive", "En Curso".equals(tState));
        model.addAttribute("stateUpcoming", "Próximamente".equals(tState));
        model.addAttribute("stateFinished", "Finalizado".equals(tState));

        List<Map<String, Object>> rankedTeams = tournamentService.calculateRankedTeams(tournament);

        List<Map<String, Object>> enrichedMatches = tournament.getMatches().stream()
                .map(tournamentService::enrichMatch)
                .collect(Collectors.toList());

        model.addAttribute("tournament", tournament);
        model.addAttribute("teams", rankedTeams);
        model.addAttribute("matches", enrichedMatches);
        model.addAttribute("teamsCount", tournament.getTeams().size());
        model.addAttribute("allTeams", teamService.findAll());
        return "tournament-detail";
    }

    @GetMapping("/tournaments/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createTournamentForm(Model model) {
        model.addAttribute("allTeams", teamService.findAll());
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

        tournamentService.createTournament(name, game, platform, mode, maxTeams,
                startDate, description, rules, imageFile);
        return "redirect:/tournaments";
    }

    @GetMapping("/admin/tournaments")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminTournaments(Model model) {
        model.addAttribute("tournaments", tournamentService.findAll());
        return "tournaments-list-admin";
    }

    @GetMapping("/admin/tournaments/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editTournamentSelection(Model model) {
        model.addAttribute("allTournaments", tournamentService.findAll());
        return "edit-tournament";
    }

    @GetMapping("/admin/tournaments/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editTournamentForm(@PathVariable Long id, Model model, Principal principal) {
        return tournamentService.findById(id).map(t -> {
            User currentUser = userService.resolveUser(principal);
            model.addAttribute("tournament", tournamentService.enrichTournament(t, currentUser));
            model.addAttribute("allTeams", teamService.findAll());
            model.addAttribute("teams", t.getTeams());

            List<Map<String, Object>> enrichedMatches = t.getMatches().stream()
                    .map(tournamentService::enrichMatch)
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

        tournamentService.editTournament(id, name, game, platform, mode, maxTeams,
                startDate, description, rules, state, imageFile);
        return "redirect:/admin/tournaments/edit/" + id;
    }

    @PostMapping("/tournaments/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return "redirect:/tournaments";
    }

    @PostMapping("/admin/tournaments/edit/{id}/teams/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addTeam(@PathVariable Long id, @RequestParam Long teamId) {
        tournamentService.addTeamToTournament(id, teamId);
        return "redirect:/admin/tournaments/edit/" + id;
    }

    @PostMapping("/admin/tournaments/edit/{id}/teams/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public String removeTeam(@PathVariable Long id, @RequestParam Long teamId) {
        tournamentService.removeTeamFromTournament(id, teamId);
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
    public ResponseEntity<?> toggleFavorite(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.resolveUser(principal);
        boolean success = tournamentService.toggleFavorite(id, user);

        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }
}