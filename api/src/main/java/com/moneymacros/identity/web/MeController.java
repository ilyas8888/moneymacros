package com.moneymacros.identity.web;

import com.moneymacros.identity.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Returns the current user; requires a session (401 otherwise, via ADR 008). */
@RestController
@Tag(name = "Me", description = "The currently authenticated user")
class MeController {

	@GetMapping("/api/me")
	@Operation(summary = "Return the authenticated user")
	UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
		return UserResponse.of(user);
	}
}
