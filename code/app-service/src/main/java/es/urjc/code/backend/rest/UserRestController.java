package es.urjc.code.backend.rest;

import es.urjc.code.backend.dto.request.UserCreateRequest;
import es.urjc.code.backend.dto.request.UserUpdateRequest;
import es.urjc.code.backend.dto.response.UserResponse;
import es.urjc.code.backend.model.PlayerMatchStats;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.MatchService;
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

import java.util.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints. Passwords are NEVER returned in responses.")
public class UserRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private MatchService matchService;

    // ── GET /api/v1/users ───────────────────────────────────
    @Operation(summary = "List all users (ADMIN)", description = "Returns a paginated list of users. Passwords are NOT included.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userService.findAll(pageable);
        Page<UserResponse> responsePage = userPage.map(UserResponse::new);

        return ResponseEntity.ok(responsePage);
    }

    // ── GET /api/v1/users/{id} ──────────────────────────────
    @Operation(summary = "Get user by ID (own user or ADMIN)", description = "Returns user details without password.")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id, Authentication auth) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        // Only allow own user or admin
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        User requestingUser = userService.resolveUser(auth.getName());
        if (!isAdmin && (requestingUser == null || !requestingUser.getId().equals(id))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(new UserResponse(opt.get()));
    }

    // ── PUT /api/v1/users/{id} ──────────────────────────────
    @Operation(summary = "Update user profile (own user or ADMIN)")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                          @RequestBody UserUpdateRequest body,
                                                          Authentication auth) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        User requestingUser = userService.resolveUser(auth.getName());
        if (!isAdmin && (requestingUser == null || !requestingUser.getId().equals(id))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            User user = opt.get();
            if (body.getNickname() != null) user.setNickname(body.getNickname());
            if (body.getUniversity() != null) user.setUniversity(body.getUniversity());
            if (body.getName() != null) user.setName(body.getName());
            userService.save(user);
            return ResponseEntity.ok(new UserResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── DELETE /api/v1/users/{id} ───────────────────────────
    @Operation(summary = "Delete user (ADMIN)")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/v1/users/{id}/stats ────────────────────────
    @Operation(summary = "Get user statistics for charts", description = "Returns KDA, win rate, and match history data for chart rendering.")
    @GetMapping("/{id}/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long id, Authentication auth) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        User user = opt.get();
        List<PlayerMatchStats> stats = matchService.findStatsByPlayer(user);

        int totalKills = 0, totalDeaths = 0, totalAssists = 0, matchesWithStats = 0;
        for (PlayerMatchStats s : stats) {
            totalKills += s.getKills();
            totalDeaths += s.getDeaths();
            totalAssists += s.getAssists();
            matchesWithStats++;
        }

        double kda = totalDeaths > 0
                ? Math.round(((totalKills + totalAssists) / (double) totalDeaths) * 100.0) / 100.0
                : totalKills + totalAssists;

        int wins = 0, gamesPlayed = 0;
        if (user.getTeam() != null) {
            wins = user.getTeam().getWins();
            gamesPlayed = user.getTeam().getMatchesPlayed();
        }

        int winRate = gamesPlayed > 0 ? (int) Math.round((wins * 100.0) / gamesPlayed) : 0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", user.getId());
        response.put("nickname", user.getNickname());
        response.put("totalKills", totalKills);
        response.put("totalDeaths", totalDeaths);
        response.put("totalAssists", totalAssists);
        response.put("kda", kda);
        response.put("matchesWithStats", matchesWithStats);
        response.put("teamWins", wins);
        response.put("teamMatchesPlayed", gamesPlayed);
        response.put("winRate", winRate);

        return ResponseEntity.ok(response);
    }


    // ── GET /api/v1/users/{id}/image ────────────────────────
    @Operation(summary = "Get user avatar image")
    @GetMapping(value = "/{id}/image", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> getUserImage(@PathVariable Long id) {
        try {
            Optional<User> opt = userService.findById(id);
            if (opt.isEmpty() || opt.get().getImageFile() == null) return ResponseEntity.notFound().build();
            byte[] imageBytes = opt.get().getImageFile().getBytes(1, (int) opt.get().getImageFile().length());
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
