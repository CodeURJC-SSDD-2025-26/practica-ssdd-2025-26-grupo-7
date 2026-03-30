package es.urjc.code.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import es.urjc.code.backend.model.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
}