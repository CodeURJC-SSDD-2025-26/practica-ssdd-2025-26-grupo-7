package es.urjc.code.backend.repository;

import es.urjc.code.backend.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import es.urjc.code.backend.model.Match;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournamentId(Long tournamentId);
    List<Match> findByLocalTeamOrAwayTeam(Team localTeam, Team awayTeam);
}