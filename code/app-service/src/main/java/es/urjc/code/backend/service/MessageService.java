package es.urjc.code.backend.service;

import es.urjc.code.backend.model.Message;
import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    public Message save(Message message) {
        return messageRepository.save(message);
    }

    public Optional<Message> findById(Long id) {
        return messageRepository.findById(id);
    }

    public void delete(Message message) {
        messageRepository.delete(message);
    }

    public List<Message> findByRecipientOrderBySentAtDesc(User user) {
        return messageRepository.findByRecipientOrderBySentAtDesc(user);
    }

    public org.springframework.data.domain.Page<Message> findByRecipient(User user, org.springframework.data.domain.Pageable pageable) {
        return messageRepository.findByRecipientOrderBySentAtDesc(user, pageable);
    }
}
