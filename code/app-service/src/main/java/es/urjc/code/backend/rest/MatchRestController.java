package es.urjc.code.backend.rest;

import es.urjc.code.backend.dto.request.MatchCreateRequest;
import es.urjc.code.backend.dto.request.MatchUpdateRequest;
import es.urjc.code.backend.dto.response.MatchResponse;
import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public ResponseEntity<Page<MatchResponse>> listMatches(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by tournament ID") @RequestParam(required = false) Long tournamentId,
            @Parameter(description = "Filter by state") @RequestParam(required = false) String state) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Match> matchPage = matchService.findWithFilters(tournamentId, state, pageable);
        Page<MatchResponse> responsePage = matchPage.map(MatchResponse::new);

        return ResponseEntity.ok(responsePage);
    }

    // ── GET /api/v1/matches/{id} ────────────────────────────
    @Operation(summary = "Get match details")
    @ApiResponse(responseCode = "200", description = "Match found")
    @ApiResponse(responseCode = "404", description = "Match not found")
    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Long id) {
        Optional<Match> opt = matchService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new MatchResponse(opt.get()));
    }

    // ── POST /api/v1/matches ────────────────────────────────
    @Operation(summary = "Create a match (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Match created")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatchResponse> createMatch(@Valid @RequestBody MatchCreateRequest body) {
        try {
            Match saved = matchService.createMatch(
                    body.getTournamentId(),
                    body.getLocalTeamId(),
                    body.getAwayTeamId(),
                    body.getPhase() != null ? body.getPhase() : "",
                    body.getFormat() != null ? body.getFormat() : "BO1",
                    body.getMatchDate() != null ? body.getMatchDate() : "",
                    "",
                    body.getNotes() != null ? body.getNotes() : ""
            );

            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}").buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(new MatchResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // ── PUT /api/v1/matches/{id} ────────────────────────────
    @Operation(summary = "Update a match (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Match updated")
    @ApiResponse(responseCode = "404", description = "Match not found")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatchResponse> updateMatch(@PathVariable Long id,
                                                           @Valid @RequestBody MatchUpdateRequest body) {
        Optional<Match> opt = matchService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Match m = opt.get();
        Long localTeamId = body.getLocalTeamId() != null ? body.getLocalTeamId()
                : (m.getLocalTeam() != null ? m.getLocalTeam().getId() : null);
        Long awayTeamId = body.getAwayTeamId() != null ? body.getAwayTeamId()
                : (m.getAwayTeam() != null ? m.getAwayTeam().getId() : null);

        try {
            matchService.updateMatch(
                    id, localTeamId, awayTeamId,
                    body.getPhase() != null ? body.getPhase() : m.getPhase(),
                    body.getFormat() != null ? body.getFormat() : m.getFormat(),
                    body.getState() != null ? body.getState() : m.getState(),
                    body.getMatchDate() != null ? body.getMatchDate() : m.getMatchDate(),
                    "",
                    body.getScoreLocal() != null ? body.getScoreLocal() : m.getScoreLocal(),
                    body.getScoreAway() != null ? body.getScoreAway() : m.getScoreAway(),
                    body.getNotes() != null ? body.getNotes() : m.getNotes(),
                    new HashMap<>()
            );
            Optional<Match> updated = matchService.findById(id);
            return updated.map(match -> ResponseEntity.ok(new MatchResponse(match)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
}
