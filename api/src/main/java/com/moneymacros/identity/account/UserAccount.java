package com.moneymacros.identity.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * A user account: the row in the {@code users} table. The email is normalised
 * (trimmed and lower-cased with {@link Locale#ROOT}) before it is stored, so the
 * unique constraint catches duplicates regardless of casing or whitespace.
 */
@Entity
@Table(name = "users")
public class UserAccount {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected UserAccount() {
		// for JPA
	}

	public UserAccount(String email, String passwordHash) {
		this.id = UUID.randomUUID();
		this.email = normaliseEmail(email);
		this.passwordHash = passwordHash;
		this.createdAt = Instant.now();
	}

	/** Trim and lower-case with {@link Locale#ROOT} (never the default locale). */
	public static String normaliseEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
