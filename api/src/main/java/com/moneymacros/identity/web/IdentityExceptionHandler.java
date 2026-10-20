package com.moneymacros.identity.web;

import com.moneymacros.identity.account.EmailAlreadyUsedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sign-up and log-in error cases as RFC 9457 Problem Details (ADR 008). Scoped to
 * {@link AuthController} with {@code assignableTypes}, so the "Invalid email or
 * password." 401 and the duplicate-email 409 stay specific to auth and are never
 * returned for other modules' controllers (which arrive from Day 8). Ordered ahead
 * of the shared {@code GlobalExceptionHandler} (lowest precedence), so its
 * catch-all cannot turn these into a 500.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class IdentityExceptionHandler {

	@ExceptionHandler(EmailAlreadyUsedException.class)
	ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT, "An account already exists for this email.");
		body.setTitle("Email already in use");
		return body;
	}

	/**
	 * Both a wrong password and an unknown email reach here as the same
	 * {@link AuthenticationException}, so the response is identical and nobody can
	 * probe which emails exist.
	 */
	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleBadCredentials(AuthenticationException ex) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(
				HttpStatus.UNAUTHORIZED, "Invalid email or password.");
		body.setTitle("Unauthorized");
		return body;
	}
}
