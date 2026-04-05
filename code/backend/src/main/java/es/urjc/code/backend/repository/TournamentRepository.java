package es.urjc.code.backend.repository;

import es.urjc.code.backend.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;


import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Query("SELECT t FROM Tournament t WHERE " +
            "(:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:game   IS NULL OR t.game  = :game)  AND " +
            "(:state  IS NULL OR t.state = :state)")
    List<Tournament> findWithFilters(
            @Param("search") String search,
            @Param("game") String game,
            @Param("state") String state);

    java.util.List<Tournament> findByTeamsContaining(es.urjc.code.backend.model.Team team);

    Optional<Tournament> findByName(String name);
}