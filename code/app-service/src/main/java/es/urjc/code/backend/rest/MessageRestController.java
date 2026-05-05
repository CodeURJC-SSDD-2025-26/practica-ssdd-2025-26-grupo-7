package es.urjc.code.backend.rest;

import es.urjc.code.backend.dto.request.MessageCreateRequest;
import es.urjc.code.backend.dto.response.MessageResponse;
import es.urjc.code.backend.model.Message;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.service.MessageService;
import es.urjc.code.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public ResponseEntity<Page<MessageResponse>> listMessages(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            Authentication auth) {

        User currentUser = userService.resolveUser(auth.getName());
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageService.findByRecipient(currentUser, pageable);
        Page<MessageResponse> responsePage = messagePage.map(MessageResponse::new);

        return ResponseEntity.ok(responsePage);
    }

    // ── POST /api/v1/messages ───────────────────────────────
    @Operation(summary = "Send a message (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Message sent")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> sendMessage(@RequestBody MessageCreateRequest body,
                                                           Authentication auth) {
        Long recipientId = body.getRecipientId();
        if (recipientId == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<User> recipientOpt = userService.findById(recipientId);
        if (recipientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User sender = userService.resolveUser(auth.getName());
        Message message = new Message(
                sender,
                recipientOpt.get(),
                body.getSubject() != null ? body.getSubject() : "",
                body.getContent() != null ? body.getContent() : ""
        );

        Message saved = messageService.save(message);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(new MessageResponse(saved));
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
}
