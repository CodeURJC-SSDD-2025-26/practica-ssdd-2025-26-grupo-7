package es.urjc.code.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
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
				// PUBLIC PAGES
				.requestMatchers("/").permitAll()
				.requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
				.requestMatchers("/login", "/loginerror", "/register").permitAll()
				.requestMatchers("/teams").permitAll()
				.requestMatchers("/tournaments").permitAll()
				// PRIVATE PAGES
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers("/profile").hasAnyRole("USER", "ADMIN")
				.requestMatchers("/favourite").hasAnyRole("USER", "ADMIN")
				.anyRequest().permitAll())
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
