package es.urjc.code.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import es.urjc.code.backend.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
}