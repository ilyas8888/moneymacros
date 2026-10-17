package com.moneymacros.identity.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Server-side sessions in an HttpOnly cookie plus a CSRF token for the SPA
 * (ADR 003). No form login and no HTTP Basic, so the browser never shows a
 * native login dialog. 401 and 403 come back as Problem Details (ADR 008).
 */
@Configuration
class SecurityConfig {

	/**
	 * Argon2id at OWASP's minimum settings: 19 MiB memory (19456 KiB), 2
	 * iterations, parallelism 1, 16-byte salt, 32-byte hash. Hashes therefore
	 * start with {@code $argon2id$}.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		// Default hideUserNotFoundExceptions=true: an unknown email surfaces as the
		// same BadCredentialsException as a wrong password, and the provider still
		// hashes a dummy password so the timing does not reveal which emails exist.
		return new ProviderManager(provider);
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http,
			AuthenticationEntryPoint authenticationEntryPoint,
			AccessDeniedHandler accessDeniedHandler) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/ping",
								"/api/auth/csrf",
								"/api/auth/signup",
								"/api/auth/login",
								"/actuator/health",
								"/v3/api-docs/**",
								"/swagger-ui/**")
						.permitAll()
						.anyRequest().authenticated())
				// CSRF for SPAs: XSRF-TOKEN cookie echoed in the X-XSRF-TOKEN header.
				.csrf(csrf -> csrf.spa())
				// Spring Session owns the store; Spring Security only reads/writes it.
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				// No native login dialog and no login form.
				.httpBasic(httpBasic -> httpBasic.disable())
				.formLogin(formLogin -> formLogin.disable())
				// Logout is handled by our own /api/auth/logout endpoint.
				.logout(logout -> logout.disable())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}
}
