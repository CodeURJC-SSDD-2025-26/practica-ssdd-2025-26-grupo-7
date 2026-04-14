package es.urjc.code.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.authorizeHttpRequests(authorize -> authorize
				// PUBLIC
				.requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/assets/**").permitAll()
				.requestMatchers("/", "/login", "/loginerror", "/register").permitAll()
				.requestMatchers("/tournaments", "/tournaments/{id}").permitAll()
				.requestMatchers("/teams", "/teams/{id}").permitAll()
				.requestMatchers("/matches", "/matches/{id}").permitAll()
				.requestMatchers("/error", "/error-403").permitAll()
				.requestMatchers("/api/teams/*/players").permitAll()
				// ADMIN
				.requestMatchers("/admin/**").hasRole("ADMIN")
				// AUTHENTICATED
				.requestMatchers("/profile", "/profile/edit").hasAnyRole("USER", "ADMIN")
				.requestMatchers("/profile/{id}").hasAnyRole("USER", "ADMIN")
				.requestMatchers("/favourites").hasAnyRole("USER", "ADMIN")
				.requestMatchers("/teams/create", "/teams/*/edit", "/teams/*/delete").hasAnyRole("USER", "ADMIN")
				.requestMatchers("/teams/*/add-player", "/teams/*/remove-player").hasAnyRole("USER", "ADMIN")
				.requestMatchers("/tournaments/*/toggle-favorite").hasAnyRole("USER", "ADMIN")
				.requestMatchers("/messages/**").hasAnyRole("USER", "ADMIN")

				.anyRequest().authenticated())
				.formLogin(formLogin -> formLogin
						.loginPage("/login")
						.failureUrl("/login?error=true")
						.defaultSuccessUrl("/")
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/")
						.permitAll());

		return http.build();
	}

}
