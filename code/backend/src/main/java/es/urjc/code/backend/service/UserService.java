package es.urjc.code.backend.service;

import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.Tournament;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.TeamRepository;
import es.urjc.code.backend.repository.TournamentRepository;
import es.urjc.code.backend.repository.UserRepository;

import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByName(String name) {
        return userRepository.findByName(name);
    }

    public Optional<User> findByNickname(String nickname) {
        return userRepository.findByNickname(nickname);
    }

    public List<User> findAvailableUsers() {
        return userRepository.findByTeamIsNullAndEnabledTrue();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public long count() {
        return userRepository.count();
    }

    /**
     * Resolves the currently authenticated user from the principal.
     */
    public User resolveUser(Principal principal) {
        if (principal == null)
            return null;
        return userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByName(principal.getName()))
                .orElse(null);
    }

    public User resolveUser(String username) {
        if (username == null)
            return null;
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByName(username))
                .orElse(null);
    }

    public String registerUser(String name, String nickname, String email,
            String university, String password, String confirmPassword,
            MultipartFile imageFile) throws IOException {

        if (name == null || name.trim().isEmpty()) {
            return "Name is required.";
        }
        if (nickname == null || nickname.trim().isEmpty()) {
            return "Nickname is required.";
        }
        if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return "Please enter a valid email address.";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return "Email is already in use.";
        }

        User user = new User(name, nickname, email, university,
                passwordEncoder.encode(password), new ArrayList<>(List.of("USER")));

        if (imageFile != null && !imageFile.isEmpty()) {
            user.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }

        userRepository.save(user);
        return null;
    }

    public void editProfile(User user, String nickname, String university,
            MultipartFile imageFile) throws IOException {
        user.setNickname(nickname);
        if (university != null) {
            user.setUniversity(university);
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            user.setImageFile(
                    BlobProxy.generateProxy(imageFile.getInputStream(), imageFile.getSize()));
        }
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            for (Team team : teamRepository.findAll()) {
                boolean modified = false;
                if (team.getCaptain() != null && team.getCaptain().getId().equals(user.getId())) {
                    team.setCaptain(null);
                    modified = true;
                }
                if (team.getPlayers().removeIf(u -> u.getId().equals(user.getId()))) {
                    modified = true;
                }
                if (modified) {
                    teamRepository.save(team);
                }
            }
            for (Tournament t : tournamentRepository.findAll()) {
                if (t.getCreator() != null && t.getCreator().getId().equals(user.getId())) {
                    t.setCreator(null);
                    tournamentRepository.save(t);
                }
            }
            userRepository.deleteById(id);
        });
    }

    public void updateRole(Long id, String role) {
        userRepository.findById(id).ifPresent(user -> {
            List<String> roles = new ArrayList<>();
            roles.add("USER");
            if ("ADMIN".equals(role)) {
                roles.add("ADMIN");
            } else if ("CAPTAIN".equals(role)) {
                roles.add("CAPTAIN");
                if (user.getTeam() != null) {
                    Team team = user.getTeam();
                    team.setCaptain(user);
                    teamRepository.save(team);
                }
            }
            user.setRoles(roles);
            userRepository.save(user);
        });
    }

    public void updateTeam(Long userId, Long teamId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (teamId == null || teamId == 0) {
                user.setTeam(null);
            } else {
                teamRepository.findById(teamId).ifPresent(user::setTeam);
            }
            userRepository.save(user);
        });
    }

    public List<User> findCaptains() {
        return userRepository.findAll().stream()
                .filter(User::getIsCaptain)
                .toList();
    }
}
