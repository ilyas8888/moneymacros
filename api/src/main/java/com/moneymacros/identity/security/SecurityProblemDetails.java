package com.moneymacros.identity.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/**
 * Builds the RFC 9457 Problem Details that Spring Security's 401 and 403 return,
 * so they match the rest of the API's error format (ADR 008). Kept in one place
 * so the entry point and the access-denied handler stay consistent.
 */
final class SecurityProblemDetails {

	private SecurityProblemDetails() {
	}

	static ProblemDetail unauthorized() {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(
				HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource.");
		body.setTitle("Unauthorized");
		return body;
	}

	static ProblemDetail forbidden() {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(
				HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
		body.setTitle("Forbidden");
		return body;
	}

	/** Writes the Problem Detail as {@code application/problem+json} with its status. */
	static void write(HttpServletResponse response, ProblemDetail body, ObjectMapper objectMapper)
			throws IOException {
		response.setStatus(body.getStatus());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), body);
	}
}
