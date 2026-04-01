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

    @RequestMapping("/error")
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
        if (status == null) return "An unexpected error occurred.";
        return switch (status) {
            case NOT_FOUND          -> "The page you are looking for does not exist.";
            case FORBIDDEN          -> "You do not have permission to access this resource.";
            case UNAUTHORIZED       -> "You must be logged in to access this page.";
            case METHOD_NOT_ALLOWED -> "The HTTP method used is not allowed for this endpoint.";
            case BAD_REQUEST        -> "Your request was malformed or missing required fields.";
            default                 -> "An unexpected server error occurred. Please try again later.";
        };
    }
}
