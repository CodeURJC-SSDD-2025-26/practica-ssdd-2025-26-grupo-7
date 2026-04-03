package es.urjc.code.backend.repository;
import es.urjc.code.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByName(String name);

    Optional<User> findByNickname(String nickname);

    java.util.List<User> findByTeamIsNull();
}
