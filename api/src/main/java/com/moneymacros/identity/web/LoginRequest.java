package com.moneymacros.identity.web;

import jakarta.validation.constraints.NotBlank;

/** Login body. No size rule here: the password rule is enforced at signup. */
public record LoginRequest(
		@NotBlank String email,
		@NotBlank String password) {}
