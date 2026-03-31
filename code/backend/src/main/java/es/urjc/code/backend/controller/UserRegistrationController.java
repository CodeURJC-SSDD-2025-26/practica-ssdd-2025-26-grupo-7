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
            @RequestParam String name,
            @RequestParam String nickname,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam(required = false) MultipartFile imageFile,
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

        User user = new User(name, nickname, email,
                passwordEncoder.encode(password), List.of("USER"));

        if (imageFile != null && !imageFile.isEmpty()) {
            user.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }

        userRepository.save(user);
        return "redirect:/login?registered=true";
    }

    // ADMIN 
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "user-management";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/users";
    }
}
