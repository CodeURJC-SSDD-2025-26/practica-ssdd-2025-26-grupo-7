package es.urjc.code.backend.repository;

import es.urjc.code.backend.model.Team;
import es.urjc.code.backend.model.Tournament;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

        @Query("SELECT t FROM Tournament t WHERE " +
                        "(:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                        "(:game   IS NULL OR t.game  = :game)  AND " +
                        "(:state  IS NULL OR t.state = :state)")
        Page<Tournament> findWithFilters(
                        @Param("search") String search,
                        @Param("game") String game,
                        @Param("state") String state,
                        Pageable pageable);

        List<Tournament> findByTeamsContaining(Team team);

        Optional<Tournament> findByName(String name);
}
