package es.urjc.code.backend.controller;

import java.security.Principal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import es.urjc.code.backend.repository.UserRepository;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UserRepository userRepository;

	@ModelAttribute
	public void addAttributes(Model model, HttpServletRequest request) {

		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (csrfToken != null) {
			model.addAttribute("_csrf", csrfToken);
		}

		Principal principal = request.getUserPrincipal();

		if (principal != null) {
			model.addAttribute("logged", true);
			model.addAttribute("userName", principal.getName());
            
            userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByName(principal.getName()))
                .ifPresentOrElse(
                    u -> {
                        model.addAttribute("userId", u.getId());
                        model.addAttribute("userNickname", u.getNickname());
                    },
                    () -> {
                        model.addAttribute("userId", "0");
                        model.addAttribute("userNickname", "Usuario");
                    }
                );

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            boolean isAdmin = auth != null && (
                auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ||
                auth.getName().equals("Administrador") ||
                auth.getName().equals("admin@onetapeleague.com") ||
                auth.getName().equals("admin")
            );
                
			model.addAttribute("admin", isAdmin);
		} else {
			model.addAttribute("logged", false);
		}

	}
}
