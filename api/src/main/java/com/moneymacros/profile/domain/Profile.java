package com.moneymacros.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A user's money profile: the row in the {@code profiles} table. Country and
 * currency are always set (MA / MAD for a new profile); city, household size and
 * net income stay {@code null} until the user completes the profile. The currency
 * is derived from the country (see {@link Countries}), never chosen by the client.
 */
@Entity
@Table(name = "profiles")
public class Profile {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 2)
	private String country;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 3)
	private String currency;

	@Column(length = 80)
	private String city;

	@Column(name = "household_size")
	private Integer householdSize;

	@Column(name = "net_income_minor")
	private Long netIncomeMinor;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Profile() {
		// for JPA
	}

	/** A fresh, empty profile for a user: country and currency set, the rest blank. */
	public Profile(UUID userId, String country, String currency) {
		this.id = UUID.randomUUID();
		this.userId = userId;
		this.country = country;
		this.currency = currency;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	/** Replace the editable fields; the currency is set from the country, not here. */
	public void update(String country, String currency, String city, int householdSize,
			long netIncomeMinor) {
		this.country = country;
		this.currency = currency;
		this.city = city;
		this.householdSize = householdSize;
		this.netIncomeMinor = netIncomeMinor;
		this.updatedAt = Instant.now();
	}

	/** True once city, household size and net income are all set. */
	public boolean isComplete() {
		return city != null && householdSize != null && netIncomeMinor != null;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getCountry() {
		return country;
	}

	public String getCurrency() {
		return currency;
	}

	public String getCity() {
		return city;
	}

	public Integer getHouseholdSize() {
		return householdSize;
	}

	public Long getNetIncomeMinor() {
		return netIncomeMinor;
	}
}
