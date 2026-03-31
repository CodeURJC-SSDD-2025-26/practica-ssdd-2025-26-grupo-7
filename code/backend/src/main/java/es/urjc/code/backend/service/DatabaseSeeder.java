package es.urjc.code.backend.service;

import java.util.List;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.UserRepository;

@Service
public class DatabaseSeeder {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@PostConstruct
	public void init() {
		// Insert admin user if it doesn't exist
		if (userRepository.findByName("admin").isEmpty()) {
			User admin = new User(
					"admin",
					"adminNickname",
					"admin@onetap.com",
					passwordEncoder.encode("admin"),
					List.of("USER", "ADMIN")
			);
			userRepository.save(admin);
		}

		// Insert user if it doesn't exist
		if (userRepository.findByName("user").isEmpty()) {
			User user = new User(
					"user",
					"userNickname",
					"user@onetap.com",
					passwordEncoder.encode("user"),
					List.of("USER")
			);
			userRepository.save(user);
		}
	}

}
