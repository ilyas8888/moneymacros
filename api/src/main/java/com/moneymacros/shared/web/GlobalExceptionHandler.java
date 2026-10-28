package com.moneymacros.shared.web;

import com.moneymacros.shared.FieldValidationException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 Problem Detail ({@code application/problem+json}),
 * so the whole API speaks one error format (ADR 008). Extending
 * {@link ResponseEntityExceptionHandler} means Spring's own errors (malformed
 * JSON, unknown URL, wrong method) come back as Problem Details too.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** One invalid field: which field and why. */
	public record FieldError(String field, String message) {}

	/**
	 * Let Spring Security's own exceptions through instead of mapping them here:
	 * rethrowing lets the security filter chain answer with its Problem-Detail 401
	 * or 403 (ADR 003), so the catch-all below never turns them into a 500.
	 */
	@ExceptionHandler({
			org.springframework.security.core.AuthenticationException.class,
			org.springframework.security.access.AccessDeniedException.class })
	void rethrowSecurityException(RuntimeException ex) throws Exception {
		throw ex;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		ProblemDetail body = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
		body.setTitle("Validation failed");
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
	}

	/**
	 * A module's business-rule failure, returned in the same shape as bean
	 * validation so a form shows the message under the right field whether the rule
	 * was enforced by an annotation or by a service. Being the only handler for this
	 * type across the whole API, every module throws {@link FieldValidationException}
	 * and gets one consistent 400 without copying this body.
	 */
	@ExceptionHandler(FieldValidationException.class)
	ProblemDetail handleFieldValidation(FieldValidationException ex) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
		body.setTitle("Validation failed");
		body.setProperty("errors", List.of(new FieldError(ex.getField(), ex.getMessage())));
		return body;
	}

	/**
	 * Fallback for anything not handled above. Never leaks the exception message
	 * or a stack trace: it logs the exception under a random {@code errorId} and
	 * returns only that id, so a report can be traced back to the log.
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		String errorId = UUID.randomUUID().toString();
		log.error("Unexpected error [errorId={}]", errorId, ex);
		ProblemDetail body = ProblemDetail.forStatusAndDetail(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred. Quote the errorId when reporting it.");
		body.setTitle("Internal Server Error");
		body.setProperty("errorId", errorId);
		return body;
	}
}
