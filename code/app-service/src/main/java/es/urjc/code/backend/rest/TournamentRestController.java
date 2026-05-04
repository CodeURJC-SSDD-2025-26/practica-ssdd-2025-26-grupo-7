package es.urjc.code.backend.rest;

import es.urjc.code.backend.model.Tournament;
import es.urjc.code.backend.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/tournaments")
@Tag(name = "Tournaments", description = "Endpoints for managing tournaments")
public class TournamentRestController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${utility.service.url:http://localhost:8080}")
    private String utilityServiceUrl;

    // ── GET /api/v1/tournaments ─────────────────────────────
    @Operation(summary = "List all tournaments (paginated)")
    @ApiResponse(responseCode = "200", description = "Page of tournaments returned successfully")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listTournaments(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by game") @RequestParam(required = false) String game,
            @Parameter(description = "Filter by state") @RequestParam(required = false) String state) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Tournament> tournamentPage = tournamentService.findWithFilters(null, game, state, pageable);

        List<Map<String, Object>> content = tournamentPage.getContent().stream()
                .map(this::toSummaryMap).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("page", tournamentPage.getNumber());
        response.put("size", tournamentPage.getSize());
        response.put("totalElements", tournamentPage.getTotalElements());
        response.put("totalPages", tournamentPage.getTotalPages());
        response.put("last", tournamentPage.isLast());

        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/tournaments/{id} ────────────────────────
    @Operation(summary = "Get tournament details")
    @ApiResponse(responseCode = "200", description = "Tournament found")
    @ApiResponse(responseCode = "404", description = "Tournament not found")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTournament(@PathVariable Long id) {
        Optional<Tournament> opt = tournamentService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDetailMap(opt.get()));
    }

    // ── POST /api/v1/tournaments ────────────────────────────
    @Operation(summary = "Create a tournament (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Tournament created")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createTournament(@RequestBody Map<String, Object> body) {
        try {
            Tournament saved = tournamentService.createTournament(
                    (String) body.get("name"),
                    (String) body.get("game"),
                    (String) body.get("platform"),
                    (String) body.get("mode"),
                    body.containsKey("maxTeams") ? ((Number) body.get("maxTeams")).intValue() : 16,
                    (String) body.get("startDate"),
                    (String) body.get("description"),
                    (String) body.get("rules"),
                    null
            );
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}").buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(toDetailMap(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create tournament: " + e.getMessage()));
        }
    }

    // ── PUT /api/v1/tournaments/{id} ────────────────────────
    @Operation(summary = "Update a tournament (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Tournament updated")
    @ApiResponse(responseCode = "404", description = "Tournament not found")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateTournament(@PathVariable Long id,
                                                                @RequestBody Map<String, Object> body) {
        try {
            Optional<Tournament> existing = tournamentService.findById(id);
            if (existing.isEmpty()) return ResponseEntity.notFound().build();
            Tournament t = existing.get();

            boolean updated = tournamentService.editTournament(
                    id,
                    (String) body.getOrDefault("name", t.getName()),
                    (String) body.getOrDefault("game", t.getGame()),
                    (String) body.getOrDefault("platform", t.getPlatform()),
                    (String) body.getOrDefault("mode", t.getMode()),
                    body.containsKey("maxTeams") ? ((Number) body.get("maxTeams")).intValue() : t.getMaxTeams(),
                    (String) body.getOrDefault("startDate", t.getStartDate()),
                    (String) body.getOrDefault("description", t.getDescription()),
                    (String) body.getOrDefault("rules", t.getRules()),
                    (String) body.getOrDefault("state", t.getState()),
                    null
            );
            if (!updated) return ResponseEntity.notFound().build();
            Optional<Tournament> updated2 = tournamentService.findById(id);
            return updated2.map(tt -> ResponseEntity.ok(toDetailMap(tt)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/v1/tournaments/{id} ─────────────────────
    @Operation(summary = "Delete a tournament (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Tournament deleted")
    @ApiResponse(responseCode = "404", description = "Tournament not found")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        Optional<Tournament> opt = tournamentService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/v1/tournaments/{id}/pdf ────────────────────
    @Operation(summary = "Download tournament summary as PDF")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getTournamentPdf(@PathVariable Long id) {
        Optional<Tournament> opt = tournamentService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Tournament t = opt.get();

        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("id", t.getId());
        reqBody.put("name", t.getName());
        reqBody.put("game", t.getGame());
        reqBody.put("platform", t.getPlatform());
        reqBody.put("mode", t.getMode());
        reqBody.put("maxTeams", t.getMaxTeams());
        reqBody.put("startDate", t.getStartDate());
        reqBody.put("state", t.getState());
        reqBody.put("description", t.getDescription());
        reqBody.put("rules", t.getRules());

        List<Map<String, Object>> matchList = new ArrayList<>();
        if (t.getMatches() != null) {
            for (var m : t.getMatches()) {
                Map<String, Object> mMap = new LinkedHashMap<>();
                mMap.put("matchDate", m.getMatchDate());
                mMap.put("localTeamName", m.getLocalTeam() != null ? m.getLocalTeam().getName() : "TBD");
                mMap.put("awayTeamName", m.getAwayTeam() != null ? m.getAwayTeam().getName() : "TBD");
                mMap.put("phase", m.getPhase());
                mMap.put("result", m.getResult());
                mMap.put("matchState", m.getState());
                matchList.add(mMap);
            }
        }
        reqBody.put("matches", matchList);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    utilityServiceUrl + "/api/pdf/tournament", entity, byte[].class);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment", "Tournament_" + id + ".pdf");
            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    // ── GET /api/v1/tournaments/{id}/image ──────────────────
    @Operation(summary = "Get tournament banner image")
    @GetMapping(value = "/{id}/image", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> getTournamentImage(@PathVariable Long id) {
        try {
            Optional<Tournament> opt = tournamentService.findById(id);
            if (opt.isEmpty() || opt.get().getImageFile() == null) return ResponseEntity.notFound().build();
            Tournament t = opt.get();
            byte[] imageBytes = t.getImageFile().getBytes(1, (int) t.getImageFile().length());
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Helpers ─────────────────────────────────────────────
    private Map<String, Object> toSummaryMap(Tournament t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("game", t.getGame());
        m.put("platform", t.getPlatform());
        m.put("state", t.getState());
        m.put("maxTeams", t.getMaxTeams());
        m.put("startDate", t.getStartDate());
        m.put("teamCount", t.getTeams() != null ? t.getTeams().size() : 0);
        return m;
    }

    private Map<String, Object> toDetailMap(Tournament t) {
        Map<String, Object> m = toSummaryMap(t);
        m.put("mode", t.getMode());
        m.put("description", t.getDescription());
        m.put("rules", t.getRules());
        m.put("matchCount", t.getMatches() != null ? t.getMatches().size() : 0);
        return m;
    }
}
