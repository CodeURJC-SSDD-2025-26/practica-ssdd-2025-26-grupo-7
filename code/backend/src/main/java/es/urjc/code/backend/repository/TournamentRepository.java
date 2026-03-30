package es.urjc.code.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import es.urjc.code.backend.model.Tournament;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
}