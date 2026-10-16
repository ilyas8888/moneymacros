package com.moneymacros.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An exact amount of money: an integer number of minor units (e.g. centimes)
 * plus its ISO 4217 currency (ADR 002). Immutable.
 *
 * <p>Money may be negative, because a balance can go below zero. The rule that
 * "an expense must be positive" belongs to the code that creates expenses, not
 * here.
 *
 * @param amountMinor the amount in the currency's minor units
 * @param currency    the ISO 4217 currency (never {@code null})
 */
public record Money(long amountMinor, Currency currency) {

	public Money {
		Objects.requireNonNull(currency, "currency");
	}

	/**
	 * Builds a {@code Money} from a major-unit amount, using the currency's
	 * default fraction digits (2 for MAD). The amount must be exactly
	 * representable at that scale: {@code 12.300 MAD} is accepted (it is exactly
	 * 12.30), but {@code 12.345 MAD} is rejected rather than rounded silently.
	 *
	 * @throws IllegalArgumentException if the amount has more precision than the
	 *                                  currency allows
	 */
	public static Money of(BigDecimal amount, Currency currency) {
		Objects.requireNonNull(amount, "amount");
		Objects.requireNonNull(currency, "currency");
		int digits = currency.getDefaultFractionDigits();
		BigDecimal scaled;
		try {
			scaled = amount.setScale(digits, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException e) {
			throw new IllegalArgumentException(
					"%s has more than %d decimal places for %s"
							.formatted(amount.toPlainString(), digits, currency.getCurrencyCode()),
					e);
		}
		return new Money(scaled.movePointRight(digits).longValueExact(), currency);
	}

	/** The zero amount in the given currency. */
	public static Money zero(Currency currency) {
		return new Money(0L, currency);
	}

	/**
	 * This amount plus {@code other}.
	 *
	 * @throws CurrencyMismatchException if the currencies differ
	 * @throws ArithmeticException       if the sum overflows a {@code long}
	 */
	public Money plus(Money other) {
		requireSameCurrency(other);
		return new Money(Math.addExact(amountMinor, other.amountMinor), currency);
	}

	/**
	 * This amount minus {@code other}.
	 *
	 * @throws CurrencyMismatchException if the currencies differ
	 * @throws ArithmeticException       if the difference overflows a {@code long}
	 */
	public Money minus(Money other) {
		requireSameCurrency(other);
		return new Money(Math.subtractExact(amountMinor, other.amountMinor), currency);
	}

	/**
	 * This amount multiplied by {@code factor}, rounding the result back to whole
	 * minor units with the given mode.
	 */
	public Money times(BigDecimal factor, RoundingMode mode) {
		Objects.requireNonNull(factor, "factor");
		Objects.requireNonNull(mode, "mode");
		long result = BigDecimal.valueOf(amountMinor)
				.multiply(factor)
				.setScale(0, mode)
				.longValueExact();
		return new Money(result, currency);
	}

	public boolean isNegative() {
		return amountMinor < 0;
	}

	public boolean isZero() {
		return amountMinor == 0;
	}

	/** This amount in major units, scaled to the currency's fraction digits. */
	public BigDecimal toBigDecimal() {
		return BigDecimal.valueOf(amountMinor, currency.getDefaultFractionDigits());
	}

	private void requireSameCurrency(Money other) {
		Objects.requireNonNull(other, "other");
		if (!currency.equals(other.currency)) {
			throw new CurrencyMismatchException(currency, other.currency);
		}
	}
}
