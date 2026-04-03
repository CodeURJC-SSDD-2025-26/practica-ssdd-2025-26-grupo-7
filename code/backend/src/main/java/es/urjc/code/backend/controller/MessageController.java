package es.urjc.code.backend.controller;

import es.urjc.code.backend.model.Message;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.MessageRepository;
import es.urjc.code.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/admin/messages/send")
    public String sendMessage(
            @RequestParam("recipientId") Long recipientId,
            @RequestParam("subject") String subject,
            @RequestParam("content") String content,
            Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        User sender = userRepository.findByEmail(principal.getName())
                .orElseGet(() -> userRepository.findByName(principal.getName()).orElse(null));

        if (sender == null || !sender.getIsAdmin()) {
            return "redirect:/"; // Only admins can send via this endpoint
        }

        if (recipientId == -1) {
            // Broadcast to all captains
            List<User> captains = userRepository.findAll().stream()
                    .filter(User::getIsCaptain)
                    .collect(Collectors.toList());

            for (User captain : captains) {
                Message msg = new Message(sender, captain, subject, content);
                messageRepository.save(msg);
            }
        } else {
            // Send to specific user
            userRepository.findById(recipientId).ifPresent(recipient -> {
                Message msg = new Message(sender, recipient, subject, content);
                messageRepository.save(msg);
            });
        }

        return "redirect:/admin?msgSent=true";
    }

    @PostMapping("/messages/mark-read/{id}")
    public String markAsRead(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        messageRepository.findById(id).ifPresent(msg -> {
            if (msg.getRecipient().getEmail().equals(principal.getName())) {
                msg.setRead(true);
                messageRepository.save(msg);
            }
        });
        return "redirect:/profile";
    }

    @PostMapping("/messages/delete/{id}")
    public String deleteMessage(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        messageRepository.findById(id).ifPresent(msg -> {
            if (msg.getRecipient().getEmail().equals(principal.getName())) {
                messageRepository.delete(msg);
            }
        });
        return "redirect:/profile";
    }
}
