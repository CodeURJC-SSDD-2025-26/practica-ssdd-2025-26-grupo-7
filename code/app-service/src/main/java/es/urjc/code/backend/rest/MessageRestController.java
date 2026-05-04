package es.urjc.code.backend.rest;

import es.urjc.code.backend.model.Message;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.MessageService;
import es.urjc.code.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Messages", description = "Internal messaging endpoints")
public class MessageRestController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    // ── GET /api/v1/messages ────────────────────────────────
    @Operation(summary = "List messages for authenticated user (paginated)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> listMessages(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            Authentication auth) {

        User currentUser = userService.resolveUser(auth.getName());
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Message> all = messageService.findByRecipientOrderBySentAtDesc(currentUser);
        int start = page * size;
        int end = Math.min(start + size, all.size());
        List<Message> pageContent = start < all.size() ? all.subList(start, end) : List.of();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", pageContent.stream().map(this::toMap).toList());
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", all.size());
        response.put("totalPages", (int) Math.ceil((double) all.size() / size));
        response.put("last", end >= all.size());

        return ResponseEntity.ok(response);
    }

    // ── POST /api/v1/messages ───────────────────────────────
    @Operation(summary = "Send a message (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Message sent")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> body,
                                                           Authentication auth) {
        Long recipientId = body.containsKey("recipientId")
                ? Long.valueOf(body.get("recipientId").toString()) : null;
        if (recipientId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "recipientId is required"));
        }

        Optional<User> recipientOpt = userService.findById(recipientId);
        if (recipientOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Recipient not found"));
        }

        User sender = userService.resolveUser(auth.getName());
        Message message = new Message(
                sender,
                recipientOpt.get(),
                (String) body.getOrDefault("subject", ""),
                (String) body.getOrDefault("content", "")
        );

        Message saved = messageService.save(message);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(toMap(saved));
    }

    // ── DELETE /api/v1/messages/{id} ────────────────────────
    @Operation(summary = "Delete a message (sender or ADMIN)")
    @ApiResponse(responseCode = "204", description = "Message deleted")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Message not found")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id, Authentication auth) {
        Optional<Message> opt = messageService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Message message = opt.get();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        User requestingUser = userService.resolveUser(auth.getName());

        boolean isSender = message.getSender() != null
                && requestingUser != null
                && message.getSender().getId().equals(requestingUser.getId());

        if (!isAdmin && !isSender) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        messageService.delete(message);
        return ResponseEntity.noContent().build();
    }

    // ── Helper ───────────────────────────────────────────────
    private Map<String, Object> toMap(Message m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("subject", m.getSubject());
        map.put("content", m.getContent());
        map.put("sentAt", m.getFormattedDate());
        map.put("isRead", m.isRead());
        if (m.getSender() != null) {
            map.put("senderId", m.getSender().getId());
            map.put("senderName", m.getSender().getNickname());
        }
        if (m.getRecipient() != null) {
            map.put("recipientId", m.getRecipient().getId());
            map.put("recipientName", m.getRecipient().getNickname());
        }
        return map;
    }
}
