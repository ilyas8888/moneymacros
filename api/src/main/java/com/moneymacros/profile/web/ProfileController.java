package com.moneymacros.profile.web;

import com.moneymacros.identity.AuthenticatedUser;
import com.moneymacros.profile.domain.Profile;
import com.moneymacros.profile.domain.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The current user's money profile. There is no id in the URL: both endpoints act
 * on the authenticated user, and every query is scoped to that user, so no one can
 * read or change another user's profile. Requires a session (401 otherwise).
 */
@RestController
@Tag(name = "Profile", description = "The current user's money profile")
class ProfileController {

	private final ProfileService profiles;

	ProfileController(ProfileService profiles) {
		this.profiles = profiles;
	}

	@GetMapping("/api/profile")
	@Operation(summary = "Return the current user's profile")
	ProfileResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
		return profiles.find(user.id())
				.map(ProfileResponse::of)
				.orElseGet(ProfileResponse::empty);
	}

	@PutMapping("/api/profile")
	@Operation(summary = "Save the current user's profile")
	ProfileResponse update(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody UpdateProfileRequest request) {
		Profile saved = profiles.save(user.id(), request.country(), request.city(),
				request.householdSize(), request.netMonthlyIncome().amountMinor(),
				request.netMonthlyIncome().currency());
		return ProfileResponse.of(saved);
	}
}
