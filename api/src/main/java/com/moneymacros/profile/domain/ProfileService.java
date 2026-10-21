package com.moneymacros.profile.domain;

import com.moneymacros.identity.UserRegistered;
import com.moneymacros.profile.ProfileUpdated;
import com.moneymacros.shared.Money;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the profile lifecycle: create one when a user registers, read the current
 * user's profile, and save it. Every operation is scoped to a user id, so one
 * user can never touch another's data.
 */
@Service
public class ProfileService {

	/** Hard cap on net monthly income, in minor units (1,000,000,000). */
	private static final long MAX_NET_INCOME_MINOR = 1_000_000_000L;

	private final ProfileRepository profiles;
	private final ApplicationEventPublisher events;

	ProfileService(ProfileRepository profiles, ApplicationEventPublisher events) {
		this.profiles = profiles;
		this.events = events;
	}

	/**
	 * Create the empty profile for a freshly registered user, synchronously inside
	 * the sign-up transaction (ADR 007): country MA, currency MAD, the rest blank.
	 */
	@EventListener
	void onUserRegistered(UserRegistered event) {
		Currency currency = Countries.currencyFor(Countries.DEFAULT);
		profiles.save(new Profile(event.userId(), Countries.DEFAULT, currency.getCurrencyCode()));
	}

	/** The user's profile, if a row exists. Accounts created before Day 8 have none. */
	@Transactional(readOnly = true)
	public Optional<Profile> find(UUID userId) {
		return profiles.findByUserId(userId);
	}

	/**
	 * Save the user's country, city, household size and net monthly income, creating
	 * the row if it is missing, then publish {@link ProfileUpdated}. The currency is
	 * derived from the country; a mismatching or out-of-range amount is a 400.
	 */
	@Transactional
	public Profile save(UUID userId, String country, String city, int householdSize,
			long netIncomeMinor, String currencyCode) {
		Currency currency = Countries.currencyFor(country);
		if (!currency.getCurrencyCode().equals(currencyCode)) {
			throw new InvalidProfileException("netMonthlyIncome",
					"currency must be " + currency.getCurrencyCode() + " for country " + country);
		}
		if (netIncomeMinor < 0 || netIncomeMinor > MAX_NET_INCOME_MINOR) {
			throw new InvalidProfileException("netMonthlyIncome",
					"net monthly income must be between 0 and " + MAX_NET_INCOME_MINOR + " minor units");
		}
		Profile profile = profiles.findByUserId(userId)
				.orElseGet(() -> new Profile(userId, country, currency.getCurrencyCode()));
		profile.update(country, currency.getCurrencyCode(), city, householdSize, netIncomeMinor);
		Profile saved = profiles.save(profile);
		events.publishEvent(new ProfileUpdated(userId, new Money(netIncomeMinor, currency)));
		return saved;
	}
}
