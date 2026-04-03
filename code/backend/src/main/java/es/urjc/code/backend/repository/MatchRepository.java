package es.urjc.code.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import es.urjc.code.backend.model.Match;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournamentId(Long tournamentId);
    List<Match> findByLocalTeamOrAwayTeam(es.urjc.code.backend.model.Team localTeam, es.urjc.code.backend.model.Team awayTeam);
}