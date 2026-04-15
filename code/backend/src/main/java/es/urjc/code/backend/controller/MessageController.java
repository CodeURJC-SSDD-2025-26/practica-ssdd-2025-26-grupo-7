package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Message;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.MessageService;
import es.urjc.code.backend.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @PostMapping("/admin/messages/send")
    public String sendMessage(
            @RequestParam("recipientId") Long recipientId,
            @RequestParam("subject") String subject,
            @RequestParam("content") String content,
            Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        User sender = userService.resolveUser(principal);

        if (sender == null || !sender.getIsAdmin()) {
            return "redirect:/";
        }

        if (recipientId == -1) {
            List<User> captains = userService.findCaptains();

            for (User captain : captains) {
                Message msg = new Message(sender, captain, subject, content);
                messageService.save(msg);
            }
        } else {
            userService.findById(recipientId).ifPresent(recipient -> {
                Message msg = new Message(sender, recipient, subject, content);
                messageService.save(msg);
            });
        }

        return "redirect:/admin?msgSent=true";
    }

    @PostMapping("/messages/mark-read/{id}")
    public String markAsRead(@PathVariable Long id, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        messageService.findById(id).ifPresent(msg -> {
            if (msg.getRecipient().getEmail().equals(principal.getName())) {
                msg.setRead(true);
                messageService.save(msg);
            }
        });
        return "redirect:/profile";
    }

    @PostMapping("/messages/delete/{id}")
    public String deleteMessage(@PathVariable Long id, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        messageService.findById(id).ifPresent(msg -> {
            if (msg.getRecipient().getEmail().equals(principal.getName())) {
                messageService.delete(msg);
            }
        });
        return "redirect:/profile";
    }
}
