package com.moneymacros.profile.web;

import com.moneymacros.shared.MoneyDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A full profile update: all four fields are replaced at once (a partial update
 * would be a PATCH, which the MVP does not need). City is trimmed before
 * validation. Any {@code id} or {@code userId} in the body is ignored — the
 * endpoint always acts on the authenticated user.
 *
 * <p>The currency inside {@code netMonthlyIncome} is checked against the country
 * in the service, not here, because it is a cross-field rule.
 */
public record UpdateProfileRequest(
		@NotBlank @Pattern(regexp = "MA", message = "country must be MA for the MVP") String country,
		@NotBlank @Size(min = 1, max = 80) String city,
		@NotNull @Min(1) @Max(12) Integer householdSize,
		@NotNull MoneyDto netMonthlyIncome) {

	public UpdateProfileRequest {
		if (city != null) {
			city = city.trim();
		}
	}
}
