package es.urjc.code.backend.rest;

import es.urjc.code.backend.model.PlayerMatchStats;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.MatchService;
import es.urjc.code.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<Map<String, Object>> listUsers(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        List<User> all = userService.findAll();
        int start = page * size;
        int end = Math.min(start + size, all.size());
        List<User> pageContent = start < all.size() ? all.subList(start, end) : List.of();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", pageContent.stream().map(this::toSafeMap).toList());
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", all.size());
        response.put("totalPages", (int) Math.ceil((double) all.size() / size));
        response.put("last", end >= all.size());

        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/users/{id} ──────────────────────────────
    @Operation(summary = "Get user by ID (own user or ADMIN)", description = "Returns user details without password.")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id, Authentication auth) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        // Only allow own user or admin
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        User requestingUser = userService.resolveUser(auth.getName());
        if (!isAdmin && (requestingUser == null || !requestingUser.getId().equals(id))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only view your own profile"));
        }

        return ResponseEntity.ok(toSafeMap(opt.get()));
    }

    // ── PUT /api/v1/users/{id} ──────────────────────────────
    @Operation(summary = "Update user profile (own user or ADMIN)")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id,
                                                          @RequestBody Map<String, Object> body,
                                                          Authentication auth) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        User requestingUser = userService.resolveUser(auth.getName());
        if (!isAdmin && (requestingUser == null || !requestingUser.getId().equals(id))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only edit your own profile"));
        }

        try {
            User user = opt.get();
            if (body.containsKey("nickname")) user.setNickname((String) body.get("nickname"));
            if (body.containsKey("university")) user.setUniversity((String) body.get("university"));
            if (body.containsKey("name")) user.setName((String) body.get("name"));
            userService.save(user);
            return ResponseEntity.ok(toSafeMap(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
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

    // ── Helper: Safe map WITHOUT password ───────────────────
    private Map<String, Object> toSafeMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("nickname", u.getNickname());
        m.put("email", u.getEmail());
        m.put("university", u.getUniversity());
        m.put("roles", u.getRoles());
        m.put("enabled", u.isEnabled());
        // password is intentionally NOT included
        if (u.getTeam() != null) {
            m.put("teamId", u.getTeam().getId());
            m.put("teamName", u.getTeam().getName());
        }
        return m;
    }
}
