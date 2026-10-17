package com.moneymacros.identity.account;

import com.moneymacros.identity.AuthenticatedUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Verifies credentials through Spring Security's {@link AuthenticationManager}
 * (a {@code DaoAuthenticationProvider}), so the password check and its
 * timing-attack mitigation are never hand-written. On success it returns the
 * lightweight principal to store in the session.
 */
@Service
public class LoginService {

	private final AuthenticationManager authenticationManager;

	LoginService(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	/**
	 * @throws org.springframework.security.core.AuthenticationException on a wrong
	 *     password or an unknown email (the same {@code BadCredentialsException}
	 *     for both, so callers cannot probe which emails exist).
	 */
	public AuthenticatedUser authenticate(String email, String rawPassword) {
		Authentication result = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(email, rawPassword));
		UserAccountDetails details = (UserAccountDetails) result.getPrincipal();
		return new AuthenticatedUser(details.getId(), details.getUsername());
	}
}
