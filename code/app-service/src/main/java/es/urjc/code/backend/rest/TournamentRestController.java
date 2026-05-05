package es.urjc.code.backend.rest;

import es.urjc.code.backend.dto.request.TournamentCreateRequest;
import es.urjc.code.backend.dto.request.TournamentUpdateRequest;
import es.urjc.code.backend.dto.response.TournamentResponse;
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
    public ResponseEntity<Page<TournamentResponse>> listTournaments(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by game") @RequestParam(required = false) String game,
            @Parameter(description = "Filter by state") @RequestParam(required = false) String state) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Tournament> tournamentPage = tournamentService.findWithFilters(null, game, state, pageable);
        Page<TournamentResponse> responsePage = tournamentPage.map(TournamentResponse::new);

        return ResponseEntity.ok(responsePage);
    }

    // ── GET /api/v1/tournaments/{id} ────────────────────────
    @Operation(summary = "Get tournament details")
    @ApiResponse(responseCode = "200", description = "Tournament found")
    @ApiResponse(responseCode = "404", description = "Tournament not found")
    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournament(@PathVariable Long id) {
        Optional<Tournament> opt = tournamentService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new TournamentResponse(opt.get()));
    }

    // ── POST /api/v1/tournaments ────────────────────────────
    @Operation(summary = "Create a tournament (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Tournament created")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TournamentResponse> createTournament(@RequestBody TournamentCreateRequest body) {
        try {
            Tournament saved = tournamentService.createTournament(
                    body.getName(),
                    body.getGame(),
                    body.getPlatform(),
                    body.getMode(),
                    body.getMaxTeams() != 0 ? body.getMaxTeams() : 16,
                    body.getStartDate(),
                    body.getDescription(),
                    body.getRules(),
                    null
            );
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}").buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(new TournamentResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── PUT /api/v1/tournaments/{id} ────────────────────────
    @Operation(summary = "Update a tournament (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Tournament updated")
    @ApiResponse(responseCode = "404", description = "Tournament not found")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TournamentResponse> updateTournament(@PathVariable Long id,
                                                                 @RequestBody TournamentUpdateRequest body) {
        try {
            Optional<Tournament> existing = tournamentService.findById(id);
            if (existing.isEmpty()) return ResponseEntity.notFound().build();
            Tournament t = existing.get();

            boolean updated = tournamentService.editTournament(
                    id,
                    body.getName() != null ? body.getName() : t.getName(),
                    body.getGame() != null ? body.getGame() : t.getGame(),
                    body.getPlatform() != null ? body.getPlatform() : t.getPlatform(),
                    body.getMode() != null ? body.getMode() : t.getMode(),
                    body.getMaxTeams() != null ? body.getMaxTeams() : t.getMaxTeams(),
                    body.getStartDate() != null ? body.getStartDate() : t.getStartDate(),
                    body.getDescription() != null ? body.getDescription() : t.getDescription(),
                    body.getRules() != null ? body.getRules() : t.getRules(),
                    body.getState() != null ? body.getState() : t.getState(),
                    null
            );
            if (!updated) return ResponseEntity.notFound().build();
            Optional<Tournament> updated2 = tournamentService.findById(id);
            return updated2.map(tt -> ResponseEntity.ok(new TournamentResponse(tt)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
}
