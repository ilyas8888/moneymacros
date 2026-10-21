package com.moneymacros.profile.web;

import com.moneymacros.profile.domain.InvalidProfileException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Profile business-rule failures as RFC 9457 Problem Details (ADR 008), scoped to
 * {@link ProfileController}. The body matches bean validation's shape — title
 * "Validation failed" and an {@code errors} list naming the field — so the Day 10
 * form can show the message under the right input whether the rule was enforced by
 * an annotation or by the service. Ordered ahead of the shared
 * {@code GlobalExceptionHandler} so its catch-all cannot turn these into a 500.
 */
@RestControllerAdvice(assignableTypes = ProfileController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class ProfileExceptionHandler {

	/** One invalid field: which field and why (mirrors the bean-validation shape). */
	record FieldError(String field, String message) {}

	@ExceptionHandler(InvalidProfileException.class)
	ProblemDetail handleInvalidProfile(InvalidProfileException ex) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
		body.setTitle("Validation failed");
		body.setProperty("errors", List.of(new FieldError(ex.getField(), ex.getMessage())));
		return body;
	}
}
