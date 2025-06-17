package org.project.logout_get_or_post;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/", "/favicon.ico",
					"/login", "/logout",
					"/css/**", "/js/**", "/images/**",
					"/error"
				).permitAll()
				.anyRequest().authenticated()
			)
			.formLogin(AbstractAuthenticationFilterConfigurer::permitAll
			)
			.logout(logout->logout
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
				.permitAll()
			);
		return http.build();
	}
}