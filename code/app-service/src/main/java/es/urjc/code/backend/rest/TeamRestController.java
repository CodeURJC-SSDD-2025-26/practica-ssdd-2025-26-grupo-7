package es.urjc.code.backend.rest;

import es.urjc.code.backend.dto.request.TeamCreateRequest;
import es.urjc.code.backend.dto.request.TeamUpdateRequest;
import es.urjc.code.backend.dto.response.TeamResponse;
import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.TeamService;
import es.urjc.code.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Endpoints for managing teams")
public class TeamRestController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    // ── GET /api/v1/teams ───────────────────────────────────
    @Operation(summary = "List all teams (paginated)")
    @GetMapping
    public ResponseEntity<Page<TeamResponse>> listTeams(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search by name") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by game") @RequestParam(required = false) String game) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Team> teamPage = teamService.findWithFilters(search, game, pageable);
        Page<TeamResponse> responsePage = teamPage.map(TeamResponse::new);

        return ResponseEntity.ok(responsePage);
    }

    // ── GET /api/v1/teams/{id} ──────────────────────────────
    @Operation(summary = "Get team details")
    @ApiResponse(responseCode = "200", description = "Team found")
    @ApiResponse(responseCode = "404", description = "Team not found")
    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable Long id) {
        Optional<Team> opt = teamService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new TeamResponse(opt.get()));
    }

    // ── POST /api/v1/teams ──────────────────────────────────
    @Operation(summary = "Create a team (authenticated)")
    @ApiResponse(responseCode = "201", description = "Team created")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> createTeam(@RequestBody TeamCreateRequest body,
                                                           Authentication auth) {
        try {
            Team saved = teamService.createTeam(
                    body.getName(),
                    body.getUniversity(),
                    body.getMainGame(),
                    body.getDescription(),
                    body.getTag(),
                    null,
                    null,
                    null,
                    auth.getName()
            );
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}").buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(new TeamResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── PUT /api/v1/teams/{id} ──────────────────────────────
    @Operation(summary = "Update a team (captain or ADMIN)")
    @ApiResponse(responseCode = "200", description = "Team updated")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Team not found")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable Long id,
                                                           @RequestBody TeamUpdateRequest body,
                                                           Authentication auth) {
        Optional<Team> opt = teamService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Team team = opt.get();
        // Check authorization: must be captain or admin
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isCaptain = teamService.isCaptainOf(team, auth.getName());

        if (!isAdmin && !isCaptain) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            teamService.editTeam(
                    id,
                    body.getName() != null ? body.getName() : team.getName(),
                    body.getUniversity() != null ? body.getUniversity() : team.getUniversity(),
                    body.getMainGame() != null ? body.getMainGame() : team.getMainGame(),
                    body.getDescription() != null ? body.getDescription() : team.getDescription(),
                    body.getTag() != null ? body.getTag() : team.getTag(),
                    body.getCaptainId(),
                    null, null, null
            );
            Optional<Team> updated = teamService.findById(id);
            return updated.map(t -> ResponseEntity.ok(new TeamResponse(t)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── DELETE /api/v1/teams/{id} ───────────────────────────
    @Operation(summary = "Delete a team (captain or ADMIN)")
    @ApiResponse(responseCode = "204", description = "Team deleted")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Team not found")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id, Authentication auth) {
        Optional<Team> opt = teamService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Team team = opt.get();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isCaptain = teamService.isCaptainOf(team, auth.getName());

        if (!isAdmin && !isCaptain) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        teamService.deleteTeam(team);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/v1/teams/{id}/image ────────────────────────
    @Operation(summary = "Get team logo image")
    @GetMapping(value = "/{id}/image", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> getTeamImage(@PathVariable Long id) {
        try {
            Optional<Team> opt = teamService.findById(id);
            if (opt.isEmpty() || opt.get().getImageFile() == null) return ResponseEntity.notFound().build();
            byte[] imageBytes = opt.get().getImageFile().getBytes(1, (int) opt.get().getImageFile().length());
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
