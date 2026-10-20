package com.moneymacros.identity.account;

import com.moneymacros.identity.AuthenticatedUser;
import com.moneymacros.identity.UserRegistered;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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

	/** The PostgreSQL unique constraint on {@code users.email} (see {@code V1}). */
	private static final String EMAIL_UNIQUE_CONSTRAINT = "users_email_key";

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
		try {
			// Flush now so a duplicate hits the unique constraint here, inside this
			// method, rather than at commit. The existsByEmail check above loses the
			// race when two signups run at once; the database is the source of truth.
			accounts.saveAndFlush(account);
		} catch (DataIntegrityViolationException ex) {
			if (isEmailUniqueViolation(ex)) {
				throw new EmailAlreadyUsedException();
			}
			throw ex;
		}
		// Only after the insert succeeded: a listener must never see a phantom user.
		events.publishEvent(new UserRegistered(account.getId()));
		return new AuthenticatedUser(account.getId(), account.getEmail());
	}

	/**
	 * True only when {@code ex} was caused by violating {@link #EMAIL_UNIQUE_CONSTRAINT}.
	 * Any other integrity error (a future foreign key, a not-null, …) is left alone,
	 * so it still surfaces as a 500 rather than a misleading "email already used".
	 */
	private boolean isEmailUniqueViolation(DataIntegrityViolationException ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof org.hibernate.exception.ConstraintViolationException hibernate) {
				return EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(hibernate.getConstraintName());
			}
		}
		return false;
	}
}
