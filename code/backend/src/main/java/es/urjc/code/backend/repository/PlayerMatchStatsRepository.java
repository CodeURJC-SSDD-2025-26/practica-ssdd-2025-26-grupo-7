package es.urjc.code.backend.repository;

import es.urjc.code.backend.model.Match;
import es.urjc.code.backend.model.PlayerMatchStats;
import es.urjc.code.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerMatchStatsRepository extends JpaRepository<PlayerMatchStats, Long> {
    List<PlayerMatchStats> findByMatch(Match match);

    void deleteByMatch(Match match);

    List<PlayerMatchStats> findByPlayer(User player);
}
