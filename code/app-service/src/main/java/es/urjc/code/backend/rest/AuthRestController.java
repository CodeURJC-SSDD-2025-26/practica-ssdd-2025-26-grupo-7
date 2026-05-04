package es.urjc.code.backend.rest;

import es.urjc.code.backend.model.User;
import es.urjc.code.backend.security.JwtTokenProvider;
import es.urjc.code.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "JWT login and registration endpoints for the REST API")
public class AuthRestController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    @Operation(summary = "Login and obtain a JWT token")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .toList();

            String token = jwtTokenProvider.generateToken(userDetails.getUsername(), roles);
            return ResponseEntity.ok(Map.of("token", "Bearer " + token));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    @Operation(summary = "Register a new user and obtain a JWT token")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userService.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }

        User user = userService.registerUser(
                request.name(),
                request.nickname(),
                request.email(),
                request.university(),
                request.password()
        );

        List<String> roles = user.getRoles();
        String token = jwtTokenProvider.generateToken(user.getEmail(), roles);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("token", "Bearer " + token, "id", user.getId()));
    }

    public record LoginRequest(String username, String password) {}

    public record RegisterRequest(String name, String nickname, String email,
                                  String university, String password) {}
}
