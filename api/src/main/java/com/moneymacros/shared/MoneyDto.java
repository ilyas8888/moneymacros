package com.moneymacros.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * How money travels over the API: an integer number of minor units plus its
 * ISO 4217 currency (ADR 002), mirroring {@link Money}. Shared across modules so
 * every endpoint that returns or accepts money speaks the same
 * {@code { amountMinor, currency }} shape, in both directions.
 *
 * <p>{@code amountMinor} is a {@link Long}, not a primitive, so a request that
 * omits it is rejected as missing rather than silently read as zero (validate it
 * with {@code @Valid} on the field that carries the {@code MoneyDto}).
 */
public record MoneyDto(
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotNull Long amountMinor,
		@Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank String currency) {}
