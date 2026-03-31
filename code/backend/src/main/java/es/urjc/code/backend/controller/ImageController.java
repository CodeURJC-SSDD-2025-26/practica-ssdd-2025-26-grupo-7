package es.urjc.code.backend.controller;

import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.TournamentRepository;
import es.urjc.code.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Blob;
import java.sql.SQLException;

@RestController
public class ImageController {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    // Tournament image
    @GetMapping("/images/tournaments/{id}")
    public ResponseEntity<byte[]> getTournamentImage(@PathVariable Long id) {
        return tournamentRepository.findById(id)
                .map(t -> blobToResponse(t.getImageFile()))
                .orElse(ResponseEntity.notFound().build());
    }

    // Team image
    @GetMapping("/images/teams/{id}")
    public ResponseEntity<byte[]> getTeamImage(@PathVariable Long id) {
        return teamRepository.findById(id)
                .map(t -> blobToResponse(t.getImageFile()))
                .orElse(ResponseEntity.notFound().build());
    }

    // User image
    @GetMapping("/images/users/{id}")
    public ResponseEntity<byte[]> getUserImage(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> blobToResponse(u.getImageFile()))
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> blobToResponse(Blob blob) {
        if (blob == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] bytes = blob.getBytes(1, (int) blob.length());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(detectMediaType(bytes));
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    private MediaType detectMediaType(byte[] bytes) {
        if (bytes.length >= 4
                && bytes[0] == (byte) 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        if (bytes.length >= 3
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F') {
            return MediaType.IMAGE_GIF;
        }
        return MediaType.IMAGE_JPEG;
    }
}
