package com.moneymacros.identity.account;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads an account by email for the {@code DaoAuthenticationProvider}. Throwing
 * {@link UsernameNotFoundException} for an unknown email lets the provider hash
 * a dummy password anyway, so login time does not reveal whether the email
 * exists (and the caller only ever sees a generic bad-credentials failure).
 */
@Service
class UserAccountDetailsService implements UserDetailsService {

	private final UserAccountRepository accounts;

	UserAccountDetailsService(UserAccountRepository accounts) {
		this.accounts = accounts;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		return accounts.findByEmail(UserAccount.normaliseEmail(username))
				.map(UserAccountDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("No account for that email"));
	}
}
