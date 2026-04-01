package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.UserRepository;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class UserRegistrationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // REGISTER
    @PostMapping("/register")
    public String register(
            @RequestParam("name") String name,
            @RequestParam("nickname") String nickname,
            @RequestParam("email") String email,
            @RequestParam("university") String university,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Model model) throws IOException {

        // Basic validations
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Email is already in use.");
            return "register";
        }

        User user = new User(name, nickname, email, university,
                passwordEncoder.encode(password), new java.util.ArrayList<>(List.of("USER")));

        if (imageFile != null && !imageFile.isEmpty()) {
            user.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }

        userRepository.save(user);
        return "redirect:/login?registered=true";
    }

    @Autowired
    private es.urjc.code.backend.repository.TeamRepository teamRepository;

    // ADMIN 
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        long totalUsers = users.size();
        long activeUsers = totalUsers; // No boolean ban field yet
        long captains = users.stream().filter(User::getIsCaptain).count();
        long freeAgents = users.stream().filter(u -> !u.getHasTeam()).count();

        model.addAttribute("users", users);
        model.addAttribute("teams", teamRepository.findAll());
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("captains", captains);
        model.addAttribute("freeAgents", freeAgents);
        
        return "user-management";
    }

    @Autowired
    private es.urjc.code.backend.repository.TournamentRepository tournamentRepository;

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userRepository.findById(id).ifPresent(user -> {
            // Unlink from Teams
            for (es.urjc.code.backend.model.Team team : teamRepository.findAll()) {
                boolean modified = false;
                if (team.getCaptain() != null && team.getCaptain().getId().equals(user.getId())) {
                    team.setCaptain(null);
                    modified = true;
                }
                if (team.getPlayers().removeIf(u -> u.getId().equals(user.getId()))) {
                    modified = true;
                }
                if (modified) {
                    teamRepository.save(team);
                }
            }
            // Unlink from Tournaments
            for (es.urjc.code.backend.model.Tournament t : tournamentRepository.findAll()) {
                if (t.getCreator() != null && t.getCreator().getId().equals(user.getId())) {
                    t.setCreator(null);
                    tournamentRepository.save(t);
                }
            }
            userRepository.deleteById(id);
        });
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam("role") String role) {
        userRepository.findById(id).ifPresent(user -> {
            List<String> roles = new java.util.ArrayList<>();
            roles.add("USER");
            if ("ADMIN".equals(role)) {
                roles.add("ADMIN");
            }
            user.setRoles(roles);
            userRepository.save(user);
        });
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/team")
    public String updateTeam(@PathVariable Long id, @RequestParam(value = "teamId", required = false) Long teamId) {
        userRepository.findById(id).ifPresent(user -> {
            if (teamId == null || teamId == 0) {
                user.setTeam(null);
            } else {
                teamRepository.findById(teamId).ifPresent(team -> {
                    user.setTeam(team);
                });
            }
            userRepository.save(user);
        });
        return "redirect:/users";
    }
}
