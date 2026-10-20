package com.moneymacros.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

	private static final Currency MAD = Currency.getInstance("MAD");
	private static final Currency USD = Currency.getInstance("USD");

	@Test
	void ofConvertsMajorUnitsToMinorUnits() {
		assertThat(Money.of(new BigDecimal("12.30"), MAD).amountMinor()).isEqualTo(1230L);
	}

	@Test
	void ofAcceptsTrailingZerosWithinTheAllowedScale() {
		// "12.300" has scale 3 but is exactly 12.30 MAD, so it is accepted.
		assertThat(Money.of(new BigDecimal("12.300"), MAD).amountMinor()).isEqualTo(1230L);
	}

	@Test
	void ofRejectsMorePrecisionThanTheCurrencyAllows() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> Money.of(new BigDecimal("12.345"), MAD));
	}

	@Test
	void toBigDecimalReturnsMajorUnitsAtTheCurrencyScale() {
		assertThat(new Money(1230L, MAD).toBigDecimal()).isEqualByComparingTo("12.30");
		assertThat(new Money(1230L, MAD).toBigDecimal().scale()).isEqualTo(2);
	}

	@Test
	void timesRoundsTheResultWithTheGivenMode() {
		// 1000 centimes * 0.1015 = 101.5 centimes, HALF_UP -> 102.
		assertThat(new Money(1000L, MAD).times(new BigDecimal("0.1015"), RoundingMode.HALF_UP).amountMinor())
				.isEqualTo(102L);
		// Same product, DOWN -> 101.
		assertThat(new Money(1000L, MAD).times(new BigDecimal("0.1015"), RoundingMode.DOWN).amountMinor())
				.isEqualTo(101L);
	}

	@Test
	void plusAddsAmountsOfTheSameCurrency() {
		assertThat(new Money(1230L, MAD).plus(new Money(70L, MAD)))
				.isEqualTo(new Money(1300L, MAD));
	}

	@Test
	void minusSubtractsAmountsOfTheSameCurrency() {
		assertThat(new Money(1300L, MAD).minus(new Money(70L, MAD)))
				.isEqualTo(new Money(1230L, MAD));
	}

	@Test
	void minusMayGoBelowZero() {
		// A balance can go negative; Money does not forbid it.
		assertThat(new Money(100L, MAD).minus(new Money(250L, MAD)))
				.isEqualTo(new Money(-150L, MAD));
	}

	@Test
	void plusRejectsADifferentCurrency() {
		assertThatExceptionOfType(CurrencyMismatchException.class)
				.isThrownBy(() -> new Money(100L, MAD).plus(new Money(100L, USD)));
	}

	@Test
	void minusRejectsADifferentCurrency() {
		assertThatExceptionOfType(CurrencyMismatchException.class)
				.isThrownBy(() -> new Money(100L, MAD).minus(new Money(100L, USD)));
	}

	@Test
	void plusThrowsWhenTheSumOverflowsALong() {
		assertThatExceptionOfType(ArithmeticException.class)
				.isThrownBy(() -> new Money(Long.MAX_VALUE, MAD).plus(new Money(1L, MAD)));
	}

	@Test
	void minusThrowsWhenTheDifferenceOverflowsALong() {
		assertThatExceptionOfType(ArithmeticException.class)
				.isThrownBy(() -> new Money(Long.MIN_VALUE, MAD).minus(new Money(1L, MAD)));
	}
}
