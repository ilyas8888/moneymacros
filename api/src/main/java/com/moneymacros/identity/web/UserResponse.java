package com.moneymacros.identity.web;

import com.moneymacros.identity.AuthenticatedUser;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;

/** The public view of an account returned by signup, login and /api/me. */
public record UserResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
		@Schema(requiredMode = RequiredMode.REQUIRED) String email) {

	static UserResponse of(AuthenticatedUser user) {
		return new UserResponse(user.id(), user.email());
	}
}
