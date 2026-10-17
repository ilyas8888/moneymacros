package com.moneymacros.identity.web;

import com.moneymacros.identity.AuthenticatedUser;
import java.util.UUID;

/** The public view of an account returned by signup, login and /api/me. */
public record UserResponse(UUID id, String email) {

	static UserResponse of(AuthenticatedUser user) {
		return new UserResponse(user.id(), user.email());
	}
}
