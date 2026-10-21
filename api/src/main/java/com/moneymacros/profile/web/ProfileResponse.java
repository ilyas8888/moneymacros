package com.moneymacros.profile.web;

import com.moneymacros.profile.domain.Countries;
import com.moneymacros.profile.domain.Profile;
import com.moneymacros.shared.MoneyDto;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * The current user's profile. Country, currency and {@code complete} are always
 * present; city, household size and net income are {@code null} until the profile
 * is complete, so they are optional in the contract.
 */
public record ProfileResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) String country,
		@Schema(requiredMode = RequiredMode.REQUIRED) String currency,
		String city,
		Integer householdSize,
		MoneyDto netMonthlyIncome,
		@Schema(requiredMode = RequiredMode.REQUIRED) boolean complete) {

	/** The stored profile. */
	static ProfileResponse of(Profile profile) {
		MoneyDto income = profile.getNetIncomeMinor() == null ? null
				: new MoneyDto(profile.getNetIncomeMinor(), profile.getCurrency());
		return new ProfileResponse(profile.getCountry(), profile.getCurrency(), profile.getCity(),
				profile.getHouseholdSize(), income, profile.isComplete());
	}

	/**
	 * The default empty profile for an account created before Day 8 that has no row
	 * yet: country MA, currency MAD, nothing filled in, not complete. Returned by
	 * GET without writing anything.
	 */
	static ProfileResponse empty() {
		return new ProfileResponse(Countries.DEFAULT,
				Countries.currencyFor(Countries.DEFAULT).getCurrencyCode(), null, null, null, false);
	}
}
