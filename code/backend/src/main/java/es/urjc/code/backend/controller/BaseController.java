package es.urjc.code.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BaseController {

    @GetMapping("/")
    public String index(Model model) {
        // En template se llamará a index.html
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/tournaments")
    public String tournaments() {
        return "tournaments";
    }

    @GetMapping("/teams")
    public String teams() {
        return "teams";
    }
}
