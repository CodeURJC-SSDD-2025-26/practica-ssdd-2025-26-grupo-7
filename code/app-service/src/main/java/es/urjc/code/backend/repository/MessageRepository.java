package es.urjc.code.backend.repository;

import es.urjc.code.backend.model.Message;
import es.urjc.code.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipientOrderBySentAtDesc(User recipient);
}
