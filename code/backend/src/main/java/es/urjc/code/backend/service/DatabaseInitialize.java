package es.urjc.code.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import es.urjc.code.backend.model.*;
import es.urjc.code.backend.repository.*;

@Service
public class DatabaseInitialize implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private PlayerMatchStatsRepository statsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        // 1. Initialize Users if not present
        User adminUser = userRepository.findByEmail("admin@onetapeleague.com").orElse(null);
        if (adminUser == null) {
            adminUser = new User("Administrador", "admin", "admin@onetapeleague.com", "",
                    passwordEncoder.encode("pass123"),
                    List.of("USER", "ADMIN"));
            userRepository.save(adminUser);
        } else {
            if (!adminUser.getPassword().startsWith("$2a$")) {
                adminUser.setPassword(passwordEncoder.encode("pass123"));
                userRepository.save(adminUser);
            }
        }

        if (userRepository.findByEmail("juan@gmail.com").isEmpty()) {
            User player1 = new User("Juan Pérez", "xSniper99", "juan@gmail.com", "URJC - Móstoles",
                    passwordEncoder.encode("pass123"), List.of("USER"));
            userRepository.save(player1);
        }

        if (userRepository.findByEmail("ana@gmail.com").isEmpty()) {
            User player2 = new User("Ana Gómez", "AnaPro", "ana@gmail.com", "URJC - Móstoles",
                    passwordEncoder.encode("pass123"), List.of("USER"));
            userRepository.save(player2);
        }

        if (userRepository.findByEmail("superadmin@onetapeleague.com").isEmpty()) {
            User superAdmin = new User("Super Admin", "superadmin", "superadmin@onetapeleague.com", "",
                    passwordEncoder.encode("pass123"), List.of("USER", "ADMIN"));
            userRepository.save(superAdmin);
        }

        if (userRepository.findByEmail("pedro@gmail.com").isEmpty()) {
            User player3 = new User("Pedro Sánchez", "GamerPro77", "pedro@gmail.com", "URJC - Fuenlabrada",
                    passwordEncoder.encode("pass123"), List.of("USER"));
            userRepository.save(player3);
        }

        if (userRepository.findByEmail("marta@gmail.com").isEmpty()) {
            User player4 = new User("Marta MVP", "MartaMVP", "marta@gmail.com", "URJC - Vicálvaro",
                    passwordEncoder.encode("pass123"), List.of("USER"));
            userRepository.save(player4);
        }

        if (userRepository.findByEmail("carlos@gmail.com").isEmpty()) {
            User player5 = new User("Carlos King", "CharlieKing", "carlos@gmail.com", "URJC - Aranjuez",
                    passwordEncoder.encode("pass123"), List.of("USER"));
            userRepository.save(player5);
        }

        if (userRepository.findByEmail("lucia@gmail.com").isEmpty()) {
            User player6 = new User("Lucía Sky", "LucySky", "lucia@gmail.com", "URJC - Móstoles",
                    passwordEncoder.encode("pass123"), List.of("USER"));
            userRepository.save(player6);
        }

        if (userRepository.findByEmail("sergio@gmail.com").isEmpty()) {
            User player7 = new User("Sergio Flow", "SergiFlow", "sergio@gmail.com", "URJC - Alcorcón",
                    passwordEncoder.encode("pass123"), List.of("USER"));
            userRepository.save(player7);
        }

        // NEW PLAYERS
        // Team 1 (Rivas): Pablo, Elena, David
        createPlayerIfNotExist("Pablo Ruiz", "Pablow", "pablo@gmail.com", "URJC - Rivas");
        createPlayerIfNotExist("Elena Sanz", "EleSanz", "elena@gmail.com", "URJC - Rivas");
        createPlayerIfNotExist("David Leon", "KingDavid", "david@gmail.com", "URJC - Rivas");

        // Team 2 (Móstoles): Iván, Sofía, Raúl, Laura
        createPlayerIfNotExist("Iván Moreno", "IvanM", "ivan@gmail.com", "URJC - Móstoles");
        createPlayerIfNotExist("Sofía Rico", "SofiR", "sofia@gmail.com", "URJC - Móstoles");
        createPlayerIfNotExist("Raúl Cano", "RaulC", "raul@gmail.com", "URJC - Móstoles");

        // Team 3 (Fuenla): Daniel, Carla, Mario, Sara
        createPlayerIfNotExist("Daniel Ortiz", "DaniO", "daniel@gmail.com", "URJC - Fuenlabrada");
        createPlayerIfNotExist("Carla Feroz", "CarlaF", "carla@gmail.com", "URJC - Fuenlabrada");
        createPlayerIfNotExist("Mario Bros", "MarioB", "mario@gmail.com", "URJC - Fuenlabrada");
        createPlayerIfNotExist("Sara Mago", "SaraM", "sara@gmail.com", "URJC - Fuenlabrada");

        // Team 4 (Vicálvaro): Javi, Paula, Hugo, Nora
        createPlayerIfNotExist("Javi Lopez", "JaviL", "javi@gmail.com", "URJC - Vicálvaro");
        createPlayerIfNotExist("Paula Rubio", "PaulaR", "paula@gmail.com", "URJC - Vicálvaro");
        createPlayerIfNotExist("Hugo Sanchiz", "HugoS", "hugo@gmail.com", "URJC - Vicálvaro");
        createPlayerIfNotExist("Nora Gomi", "NoraG", "nora@gmail.com", "URJC - Vicálvaro");

        // Team 5 (Aranjuez): Alejandro, Julia, Oscar, Irene
        createPlayerIfNotExist("Alejandro Mir", "AlexM", "alex@gmail.com", "URJC - Aranjuez");
        createPlayerIfNotExist("Julia Boti", "JuliaB", "julia@gmail.com", "URJC - Aranjuez");
        createPlayerIfNotExist("Oscar Reon", "OscarR", "oscar@gmail.com", "URJC - Aranjuez");
        createPlayerIfNotExist("Irene Vela", "IreneV", "irene@gmail.com", "URJC - Aranjuez");

        // 2. Initialize Teams and Tournaments
        User admin = userRepository.findByEmail("admin@onetapeleague.com").orElse(null);
        User player1 = userRepository.findByEmail("juan@gmail.com").orElse(null);
        User player2 = userRepository.findByEmail("ana@gmail.com").orElse(null);
        User player3 = userRepository.findByEmail("pedro@gmail.com").orElse(null);
        User player4 = userRepository.findByEmail("marta@gmail.com").orElse(null);
        User player5 = userRepository.findByEmail("carlos@gmail.com").orElse(null);
        User player6 = userRepository.findByEmail("lucia@gmail.com").orElse(null);
        User player7 = userRepository.findByEmail("sergio@gmail.com").orElse(null);

        User pablo = userRepository.findByEmail("pablo@gmail.com").orElse(null);
        User elena = userRepository.findByEmail("elena@gmail.com").orElse(null);
        User david = userRepository.findByEmail("david@gmail.com").orElse(null);
        User ivan = userRepository.findByEmail("ivan@gmail.com").orElse(null);
        User sofia = userRepository.findByEmail("sofia@gmail.com").orElse(null);
        User raul = userRepository.findByEmail("raul@gmail.com").orElse(null);
        User daniel = userRepository.findByEmail("daniel@gmail.com").orElse(null);

        User carla = userRepository.findByEmail("carla@gmail.com").orElse(null);
        User mario = userRepository.findByEmail("mario@gmail.com").orElse(null);
        User sara = userRepository.findByEmail("sara@gmail.com").orElse(null);
        User javi = userRepository.findByEmail("javi@gmail.com").orElse(null);
        User paula = userRepository.findByEmail("paula@gmail.com").orElse(null);
        User hugo = userRepository.findByEmail("hugo@gmail.com").orElse(null);
        User nora = userRepository.findByEmail("nora@gmail.com").orElse(null);
        User alex = userRepository.findByEmail("alex@gmail.com").orElse(null);
        User julia = userRepository.findByEmail("julia@gmail.com").orElse(null);
        User oscar = userRepository.findByEmail("oscar@gmail.com").orElse(null);
        User irene = userRepository.findByEmail("irene@gmail.com").orElse(null);

        if (admin == null || player1 == null || player2 == null)
            return;

        // EQUIPOS
        Team team1 = teamRepository.findByName("Rivas eSports").orElse(null);
        if (team1 == null) {
            team1 = new Team("Rivas eSports", "URJC", "Valorant", "El equipo oficial del campus de Rivas.");
            team1.setCaptain(player1);
            team1.getPlayers().add(player1);
            team1.getPlayers().add(player2);
            team1.getPlayers().add(pablo);
            team1.getPlayers().add(elena);
            team1.getPlayers().add(david);
            player1.setTeam(team1);
            player2.setTeam(team1);
            if (pablo != null)
                pablo.setTeam(team1);
            if (elena != null)
                elena.setTeam(team1);
            if (david != null)
                david.setTeam(team1);

            try {
                Resource image = new ClassPathResource("static/assets/images/team-logo.png");
                team1.setImageFile(BlobProxy.generateProxy(image.getInputStream(), image.contentLength()));
            } catch (Exception e) {
            }
            teamRepository.save(team1);
        }

        Team team2 = teamRepository.findByName("Móstoles Warriors").orElse(null);
        if (team2 == null) {
            team2 = new Team("Móstoles Warriors", "URJC", "Valorant", "Guerreros del campus de Móstoles.");
            team2.setCaptain(player6);
            team2.getPlayers().add(player6);
            team2.getPlayers().add(player7);
            team2.getPlayers().add(ivan);
            team2.getPlayers().add(sofia);
            team2.getPlayers().add(raul);
            player6.setTeam(team2);
            if (player7 != null)
                player7.setTeam(team2);
            if (ivan != null)
                ivan.setTeam(team2);
            if (sofia != null)
                sofia.setTeam(team2);
            if (raul != null)
                raul.setTeam(team2);
            teamRepository.save(team2);

        }

        Team team3 = teamRepository.findByName("Fuenla Dragons").orElse(null);
        if (team3 == null) {
            team3 = new Team("Fuenla Dragons", "URJC", "League of Legends",
                    "Dragones de Fuenlabrada controlando la grieta.");
            team3.setCaptain(player3);
            team3.getPlayers().add(player3);
            team3.getPlayers().add(daniel);
            team3.getPlayers().add(carla);
            team3.getPlayers().add(mario);
            team3.getPlayers().add(sara);
            player3.setTeam(team3);
            if (daniel != null)
                daniel.setTeam(team3);
            if (carla != null)
                carla.setTeam(team3);
            if (mario != null)
                mario.setTeam(team3);
            if (sara != null)
                sara.setTeam(team3);
            teamRepository.save(team3);

        }

        Team team4 = teamRepository.findByName("Vicálvaro Vipers").orElse(null);
        if (team4 == null) {
            team4 = new Team("Vicálvaro Vipers", "URJC", "League of Legends",
                    "Víboras letales del campus de Vicálvaro.");
            team4.setCaptain(player4);
            team4.getPlayers().add(player4);
            team4.getPlayers().add(javi);
            team4.getPlayers().add(paula);
            team4.getPlayers().add(hugo);
            team4.getPlayers().add(nora);
            player4.setTeam(team4);
            if (javi != null)
                javi.setTeam(team4);
            if (paula != null)
                paula.setTeam(team4);
            if (hugo != null)
                hugo.setTeam(team4);
            if (nora != null)
                nora.setTeam(team4);
            teamRepository.save(team4);

        }

        Team team5 = teamRepository.findByName("Aranjuez Knights").orElse(null);
        if (team5 == null) {
            team5 = new Team("Aranjuez Knights", "URJC", "CS2",
                    "Caballeros de Aranjuez defendiendo el sitio de bomba.");
            team5.setCaptain(player5);
            team5.getPlayers().add(player5);
            team5.getPlayers().add(alex);
            team5.getPlayers().add(julia);
            team5.getPlayers().add(oscar);
            team5.getPlayers().add(irene);
            player5.setTeam(team5);
            if (alex != null)
                alex.setTeam(team5);
            if (julia != null)
                julia.setTeam(team5);
            if (oscar != null)
                oscar.setTeam(team5);
            if (irene != null)
                irene.setTeam(team5);
            teamRepository.save(team5);

        }

        // --- TORNEOS ---
        Tournament tournament1 = tournamentRepository.findByName("Valorant Winter Cup").orElse(null);
        if (tournament1 == null) {
            tournament1 = new Tournament(
                    "Valorant Winter Cup", "Valorant", "PC", "5v5", 16,
                    "2026-11-01", "El gran torneo de invierno de la universidad.", "Reglas estándar de Riot Games.");
            tournament1.setCreator(admin);
            tournament1.getTeams().add(team1);
            if (team2 != null)
                tournament1.getTeams().add(team2);
            try {
                Resource image = new ClassPathResource("static/assets/images/tournament-banner.png");
                tournament1.setImageFile(BlobProxy.generateProxy(image.getInputStream(), image.contentLength()));
            } catch (Exception e) {
            }
            tournamentRepository.save(tournament1);

            // Partida terminada de Valorant
            Match match1 = new Match("2026-11-05 18:00", "Fase de Grupos", tournament1, team1, team2, "BO3");
            match1.setState("Terminado");
            match1.setScoreLocal(13);
            match1.setScoreAway(10);
            match1.setResult("13 - 10");
            matchRepository.save(match1);

            // Estadísticas Match 1
            statsRepository.save(new PlayerMatchStats(match1, player1, 24, 12, 8, 285));
            statsRepository.save(new PlayerMatchStats(match1, player2, 18, 14, 12, 210));
            if (pablo != null)
                statsRepository.save(new PlayerMatchStats(match1, pablo, 15, 15, 10, 180));
            if (elena != null)
                statsRepository.save(new PlayerMatchStats(match1, elena, 20, 10, 15, 240));
            if (david != null)
                statsRepository.save(new PlayerMatchStats(match1, david, 12, 18, 5, 150));

            if (player6 != null)
                statsRepository.save(new PlayerMatchStats(match1, player6, 22, 15, 6, 250));
            if (player7 != null)
                statsRepository.save(new PlayerMatchStats(match1, player7, 18, 16, 8, 220));
            if (ivan != null)
                statsRepository.save(new PlayerMatchStats(match1, ivan, 14, 19, 4, 160));
            if (sofia != null)
                statsRepository.save(new PlayerMatchStats(match1, sofia, 16, 17, 12, 195));
            if (raul != null)
                statsRepository.save(new PlayerMatchStats(match1, raul, 10, 20, 2, 130));

        }

        Tournament tournament2 = tournamentRepository.findByName("LoL Spring Split").orElse(null);
        if (tournament2 == null) {
            tournament2 = new Tournament(
                    "LoL Spring Split", "League of Legends", "PC", "5v5", 8,
                    "2026-03-15", "La liga de primavera de League of Legends.", "Mapa: Grieta del Invocador.");
            tournament2.setCreator(admin);
            tournament2.setState("En curso");
            if (team3 != null)
                tournament2.getTeams().add(team3);
            if (team4 != null)
                tournament2.getTeams().add(team4);
            tournamentRepository.save(tournament2);

            // Partida en curso de LoL
            Match match2 = new Match("2026-03-20 19:30", "Semifinales", tournament2, team3, team4, "BO1");
            match2.setState("En curso");
            matchRepository.save(match2);
        }

        Tournament tournament3 = tournamentRepository.findByName("CS2 Campus Battle").orElse(null);
        if (tournament3 == null) {
            tournament3 = new Tournament(
                    "CS2 Campus Battle", "CS2", "PC", "5v5", 10,
                    "2026-05-20", "Enfrentamiento táctico entre campus.", "Map Pool Competitivo.");
            tournament3.setCreator(admin);
            if (team5 != null)
                tournament3.getTeams().add(team5);
            tournamentRepository.save(tournament3);

            // Partida programada de CS2
            Match match3 = new Match("2026-05-25 17:00", "Ronda 1", tournament3, team5, team2, "BO3");
            matchRepository.save(match3);
        }
    }

    private void createPlayerIfNotExist(String name, String nickname, String email, String university) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User p = new User(name, nickname, email, university,
                    passwordEncoder.encode("pass123"), List.of("USER"));
            userRepository.save(p);
        }
    }
}
