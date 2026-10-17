package com.moneymacros.identity.account;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapts a {@link UserAccount} to Spring Security's {@link UserDetails} so the
 * {@code DaoAuthenticationProvider} can verify the password. Carries the id as
 * well, so that after authentication the controller can build the lightweight
 * {@code AuthenticatedUser} principal that is actually stored in the session.
 */
public class UserAccountDetails implements UserDetails {

	private final UUID id;
	private final String email;
	private final String passwordHash;

	public UserAccountDetails(UserAccount account) {
		this.id = account.getId();
		this.email = account.getEmail();
		this.passwordHash = account.getPasswordHash();
	}

	public UUID getId() {
		return id;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// A single role is enough for now: every account is an ordinary user.
		return List.of();
	}
}
