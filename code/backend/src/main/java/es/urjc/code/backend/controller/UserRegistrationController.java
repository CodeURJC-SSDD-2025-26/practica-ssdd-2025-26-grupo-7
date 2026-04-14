package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.TeamService;
import es.urjc.code.backend.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class UserRegistrationController {

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

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

        String error = userService.registerUser(name, nickname, email, university,
                password, confirmPassword, imageFile);

        if (error != null) {
            model.addAttribute("error", error);
            return "register";
        }

        return "redirect:/login?registered=true";
    }

    // ADMIN - User Management
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        long totalUsers = users.size();
        long activeUsers = totalUsers;
        long captains = users.stream().filter(User::getIsCaptain).count();
        long freeAgents = users.stream().filter(u -> !u.getHasTeam()).count();

        model.addAttribute("users", users);
        model.addAttribute("teams", teamService.findAll());
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("captains", captains);
        model.addAttribute("freeAgents", freeAgents);

        return "user-management";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam("role") String role) {
        userService.updateRole(id, role);
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/team")
    public String updateTeam(@PathVariable Long id, @RequestParam(value = "teamId", required = false) Long teamId) {
        userService.updateTeam(id, teamId);
        return "redirect:/users";
    }
}
