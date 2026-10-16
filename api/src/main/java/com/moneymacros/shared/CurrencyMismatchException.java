package com.moneymacros.shared;

import java.util.Currency;

/**
 * Thrown when an operation combines two {@link Money} values of different
 * currencies (e.g. {@code plus}, {@code minus}). Unchecked, because mixing
 * currencies is a programming error, not a recoverable condition.
 */
public class CurrencyMismatchException extends RuntimeException {

	public CurrencyMismatchException(Currency left, Currency right) {
		super("Currency mismatch: %s vs %s".formatted(left.getCurrencyCode(), right.getCurrencyCode()));
	}
}
