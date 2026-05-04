package es.urjc.code.backend.rest;

import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/matches")
@Tag(name = "Matches", description = "Endpoints for managing matches")
public class MatchRestController {

    @Autowired
    private MatchService matchService;

    // ── GET /api/v1/matches ─────────────────────────────────
    @Operation(summary = "List all matches (paginated)")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listMatches(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by tournament ID") @RequestParam(required = false) Long tournamentId,
            @Parameter(description = "Filter by state") @RequestParam(required = false) String state) {

        List<Match> all = matchService.findAll();

        List<Match> filtered = all.stream()
                .filter(m -> tournamentId == null || (m.getTournament() != null && m.getTournament().getId().equals(tournamentId)))
                .filter(m -> state == null || state.isBlank() || state.equalsIgnoreCase(m.getState()))
                .toList();

        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<Match> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();

        List<Map<String, Object>> content = pageContent.stream().map(this::toMap).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", filtered.size());
        response.put("totalPages", (int) Math.ceil((double) filtered.size() / size));
        response.put("last", end >= filtered.size());

        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/matches/{id} ────────────────────────────
    @Operation(summary = "Get match details")
    @ApiResponse(responseCode = "200", description = "Match found")
    @ApiResponse(responseCode = "404", description = "Match not found")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMatch(@PathVariable Long id) {
        Optional<Match> opt = matchService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toMap(opt.get()));
    }

    // ── POST /api/v1/matches ────────────────────────────────
    @Operation(summary = "Create a match (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Match created")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createMatch(@RequestBody Map<String, Object> body) {
        try {
            Long tournamentId = body.containsKey("tournamentId") ? Long.valueOf(body.get("tournamentId").toString()) : null;
            Long localTeamId = body.containsKey("localTeamId") ? Long.valueOf(body.get("localTeamId").toString()) : null;
            Long awayTeamId = body.containsKey("awayTeamId") ? Long.valueOf(body.get("awayTeamId").toString()) : null;

            Match saved = matchService.createMatch(
                    tournamentId, localTeamId, awayTeamId,
                    (String) body.getOrDefault("phase", ""),
                    (String) body.getOrDefault("format", "BO1"),
                    (String) body.getOrDefault("matchDate", ""),
                    "",
                    (String) body.getOrDefault("notes", "")
            );

            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}").buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(toMap(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create match: " + e.getMessage()));
        }
    }

    // ── PUT /api/v1/matches/{id} ────────────────────────────
    @Operation(summary = "Update a match (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Match updated")
    @ApiResponse(responseCode = "404", description = "Match not found")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateMatch(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> body) {
        Optional<Match> opt = matchService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Match m = opt.get();
        Long localTeamId = body.containsKey("localTeamId") ? Long.valueOf(body.get("localTeamId").toString())
                : (m.getLocalTeam() != null ? m.getLocalTeam().getId() : null);
        Long awayTeamId = body.containsKey("awayTeamId") ? Long.valueOf(body.get("awayTeamId").toString())
                : (m.getAwayTeam() != null ? m.getAwayTeam().getId() : null);

        try {
            matchService.updateMatch(
                    id, localTeamId, awayTeamId,
                    (String) body.getOrDefault("phase", m.getPhase()),
                    (String) body.getOrDefault("format", m.getFormat()),
                    (String) body.getOrDefault("state", m.getState()),
                    (String) body.getOrDefault("matchDate", m.getMatchDate()),
                    "",
                    body.containsKey("scoreLocal") ? Integer.valueOf(body.get("scoreLocal").toString()) : m.getScoreLocal(),
                    body.containsKey("scoreAway") ? Integer.valueOf(body.get("scoreAway").toString()) : m.getScoreAway(),
                    (String) body.getOrDefault("notes", m.getNotes()),
                    new HashMap<>()
            );
            Optional<Match> updated = matchService.findById(id);
            return updated.map(match -> ResponseEntity.ok(toMap(match)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/v1/matches/{id} ─────────────────────────
    @Operation(summary = "Delete a match (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Match deleted")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        Optional<Match> opt = matchService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helper ───────────────────────────────────────────────
    private Map<String, Object> toMap(Match m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("matchDate", m.getMatchDate());
        map.put("phase", m.getPhase());
        map.put("format", m.getFormat());
        map.put("state", m.getState());
        map.put("scoreLocal", m.getScoreLocal());
        map.put("scoreAway", m.getScoreAway());
        map.put("result", m.getResult());
        map.put("notes", m.getNotes());
        if (m.getTournament() != null) {
            map.put("tournamentId", m.getTournament().getId());
            map.put("tournamentName", m.getTournament().getName());
        }
        if (m.getLocalTeam() != null) {
            map.put("localTeamId", m.getLocalTeam().getId());
            map.put("localTeamName", m.getLocalTeam().getName());
        }
        if (m.getAwayTeam() != null) {
            map.put("awayTeamId", m.getAwayTeam().getId());
            map.put("awayTeamName", m.getAwayTeam().getName());
        }
        return map;
    }
}
