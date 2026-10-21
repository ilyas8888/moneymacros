package com.moneymacros.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * How money travels over the API: an integer number of minor units plus its
 * ISO 4217 currency (ADR 002), mirroring {@link Money}. Shared across modules so
 * every endpoint that returns or accepts money speaks the same
 * {@code { amountMinor, currency }} shape, in both directions.
 */
public record MoneyDto(
		@Schema(requiredMode = RequiredMode.REQUIRED) long amountMinor,
		@Schema(requiredMode = RequiredMode.REQUIRED) String currency) {}
