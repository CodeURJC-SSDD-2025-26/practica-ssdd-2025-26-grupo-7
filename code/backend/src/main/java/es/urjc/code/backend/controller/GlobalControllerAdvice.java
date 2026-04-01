package es.urjc.code.backend.controller;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

	@ModelAttribute
	public void addAttributes(org.springframework.ui.Model model, HttpServletRequest request) {

		org.springframework.security.web.csrf.CsrfToken csrfToken = 
			(org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
		if (csrfToken != null) {
			model.addAttribute("_csrf", csrfToken);
		}

		Principal principal = request.getUserPrincipal();

		if (principal != null) {
			model.addAttribute("logged", true);
			model.addAttribute("userName", principal.getName());
            
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
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
