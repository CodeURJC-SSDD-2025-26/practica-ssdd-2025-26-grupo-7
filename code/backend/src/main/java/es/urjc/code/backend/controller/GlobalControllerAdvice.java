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
			model.addAttribute("admin", request.isUserInRole("ADMIN"));
		} else {
			model.addAttribute("logged", false);
		}

	}
}
