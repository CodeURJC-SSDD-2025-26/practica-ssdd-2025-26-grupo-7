package es.urjc.code.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import es.urjc.code.backend.model.Team;
import java.util.Optional;


public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);

    org.springframework.data.domain.Page<Team> findByNameContainingIgnoreCaseAndMainGameContainingIgnoreCase(
            String name, String mainGame, org.springframework.data.domain.Pageable pageable);
}