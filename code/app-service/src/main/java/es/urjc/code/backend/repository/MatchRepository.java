package es.urjc.code.backend.repository;

import es.urjc.code.backend.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import es.urjc.code.backend.model.Match;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournamentId(Long tournamentId);
    List<Match> findByLocalTeamOrAwayTeam(Team localTeam, Team awayTeam);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM Match m WHERE " +
            "(:tournamentId IS NULL OR m.tournament.id = :tournamentId) AND " +
            "(:state IS NULL OR m.state = :state)")
    org.springframework.data.domain.Page<Match> findWithFilters(
            @org.springframework.data.repository.query.Param("tournamentId") Long tournamentId,
            @org.springframework.data.repository.query.Param("state") String state,
            org.springframework.data.domain.Pageable pageable);
}