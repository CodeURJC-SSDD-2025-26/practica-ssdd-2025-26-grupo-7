package es.urjc.code.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.hibernate.engine.jdbc.BlobProxy;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import es.urjc.code.backend.model.*;
import es.urjc.code.backend.repository.*;

@Service
public class DatabaseInitialize {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // 1. Initialize Users if not present
        if (userRepository.findByEmail("admin@onetapeleague.com").isEmpty()) {
            User admin = new User("Administrador", "admin", "admin@onetapeleague.com", "", passwordEncoder.encode("pass123"),
                    List.of("USER", "ADMIN"));
            User player1 = new User("Juan Pérez", "xSniper99", "juan@gmail.com", "URJC - Móstoles", passwordEncoder.encode("pass123"), List.of("USER"));
            User player2 = new User("Ana Gómez", "AnaPro", "ana@gmail.com", "URJC - Móstoles", passwordEncoder.encode("pass123"), List.of("USER"));

            userRepository.save(admin);
            userRepository.save(player1);
            userRepository.save(player2);
        }

        // 2. Initialize Teams and Tournaments if none exist
        if (tournamentRepository.count() == 0) {
            User admin = userRepository.findByEmail("admin@onetapeleague.com").orElse(null);
            User player1 = userRepository.findByEmail("juan@gmail.com").orElse(null);
            User player2 = userRepository.findByEmail("ana@gmail.com").orElse(null);

            if (admin == null || player1 == null || player2 == null) return;

            Team team1 = new Team("Rivas eSports", "URJC", "Valorant", "El equipo oficial del campus de Rivas.");
            team1.setCaptain(player1);
            team1.getPlayers().add(player1);
            team1.getPlayers().add(player2);

            try {
                Resource image = new ClassPathResource("static/assets/images/team-logo.png");
                team1.setImageFile(BlobProxy.generateProxy(image.getInputStream(), image.contentLength()));
            } catch (Exception e) {
                System.out.println("Aviso: No se pudo cargar la imagen base del equipo.");
            }

            teamRepository.save(team1);

            Tournament tournament1 = new Tournament(
                    "Valorant Winter Cup", "Valorant", "PC", "5v5", 16,
                    "2026-11-01", "El gran torneo de invierno de la universidad.", "Reglas estándar de Riot Games.");
            tournament1.setCreator(admin);
            tournament1.getTeams().add(team1);

            try {
                Resource image = new ClassPathResource("static/assets/images/tournament-banner.png");
                tournament1.setImageFile(BlobProxy.generateProxy(image.getInputStream(), image.contentLength()));
            } catch (Exception e) {
                System.out.println("Aviso: No se pudo cargar la imagen base del torneo.");
            }

            tournamentRepository.save(tournament1);

            Match match1 = new Match("2026-11-05 18:00", "Fase de Grupos", tournament1, team1, null);
            matchRepository.save(match1);
        }
    }
}
