package com.moneymacros.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Signup body: a valid email and a password of 12 to 128 characters. */
public record SignupRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 12, max = 128) String password) {}
