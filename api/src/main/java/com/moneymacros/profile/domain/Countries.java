package com.moneymacros.profile.domain;

import java.util.Currency;
import java.util.Map;

/**
 * Maps a country to the currency MoneyMacros uses for it. The currency always
 * comes from the country, never from the client, so it can never drift from
 * where the user lives. The MVP supports Morocco only (MA -> MAD).
 */
public final class Countries {

	/** The default country for a brand-new profile. */
	public static final String DEFAULT = "MA";

	private static final Map<String, Currency> CURRENCY_BY_COUNTRY =
			Map.of("MA", Currency.getInstance("MAD"));

	private Countries() {
	}

	/**
	 * The currency for {@code country}.
	 *
	 * @throws InvalidProfileException (400) if the country is not supported
	 */
	public static Currency currencyFor(String country) {
		Currency currency = CURRENCY_BY_COUNTRY.get(country);
		if (currency == null) {
			throw new InvalidProfileException("country", "country must be MA for the MVP");
		}
		return currency;
	}
}
