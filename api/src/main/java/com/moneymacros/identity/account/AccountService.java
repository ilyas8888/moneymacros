package com.moneymacros.identity.account;

import com.moneymacros.identity.AuthenticatedUser;
import com.moneymacros.identity.UserRegistered;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates accounts: normalise the email, reject duplicates, hash the password
 * with Argon2id, persist, and publish {@link UserRegistered} in the same
 * transaction so synchronous listeners see a committed account (ADR 007).
 */
@Service
public class AccountService {

	private final UserAccountRepository accounts;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher events;

	AccountService(UserAccountRepository accounts, PasswordEncoder passwordEncoder,
			ApplicationEventPublisher events) {
		this.accounts = accounts;
		this.passwordEncoder = passwordEncoder;
		this.events = events;
	}

	@Transactional
	public AuthenticatedUser register(String email, String rawPassword) {
		String normalisedEmail = UserAccount.normaliseEmail(email);
		if (accounts.existsByEmail(normalisedEmail)) {
			throw new EmailAlreadyUsedException();
		}
		UserAccount account = new UserAccount(normalisedEmail, passwordEncoder.encode(rawPassword));
		accounts.save(account);
		events.publishEvent(new UserRegistered(account.getId()));
		return new AuthenticatedUser(account.getId(), account.getEmail());
	}
}
