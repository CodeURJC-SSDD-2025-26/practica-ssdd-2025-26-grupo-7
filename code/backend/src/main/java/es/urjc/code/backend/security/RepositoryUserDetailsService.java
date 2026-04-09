package es.urjc.code.backend.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import es.urjc.code.backend.model.User;
import es.urjc.code.backend.repository.UserRepository;

@Service
public class RepositoryUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		User user = userRepository.findByEmail(username)
				.orElseGet(() -> userRepository.findByNickname(username)
						.orElseGet(() -> userRepository.findByName(username)
								.orElseThrow(() -> new UsernameNotFoundException("User not found"))));

		List<GrantedAuthority> roles = new ArrayList<>();
		for (String role : user.getRoles()) {
			roles.add(new SimpleGrantedAuthority("ROLE_" + role));
		}

		if (user.getNickname().equals("admin") || user.getName().equals("Administrador")
				|| user.getEmail().equals("admin@onetapeleague.com")) {
			boolean hasAdmin = roles.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
			if (!hasAdmin) {
				roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
			}
		}

		return new org.springframework.security.core.userdetails.User(user.getEmail(),
				user.getPassword(), roles);

	}
}
