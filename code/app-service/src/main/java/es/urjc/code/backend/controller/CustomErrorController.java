package es.urjc.code.backend.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping(value = "/error", produces = "text/html")
    public String handleError(HttpServletRequest request, Model model) {

        Object statusAttr  = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageAttr = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int code = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : 500;
        String message = resolveMessage(code, messageAttr);

        model.addAttribute("errorCode",    code);
        model.addAttribute("errorMessage", message);

        model.addAttribute("is404", code == 404);
        model.addAttribute("is403", code == 403);
        model.addAttribute("is500", code == 500);

        return "error";
    }

    // Private helper
    private String resolveMessage(int code, Object raw) {
        if (raw != null && !raw.toString().isBlank()) {
            return raw.toString();
        }
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) return "Ha ocurrido un error inesperado.";
        return switch (status) {
            case NOT_FOUND          -> "La página que buscas no existe.";
            case FORBIDDEN          -> "No tienes permiso para acceder a este recurso.";
            case UNAUTHORIZED       -> "Debes iniciar sesión para acceder a esta página.";
            case METHOD_NOT_ALLOWED -> "El método HTTP utilizado no está permitido en esta URL.";
            case BAD_REQUEST        -> "Tu petición era incorrecta o faltaban campos obligatorios.";
            default                 -> "Ha ocurrido un error inesperado en el servidor. Por favor, inténtalo de nuevo más tarde.";
        };
    }
}
