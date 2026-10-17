package com.moneymacros.identity.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Returns a 401 as a Problem Detail (ADR 008) when an unauthenticated request
 * hits a protected endpoint. It writes no {@code WWW-Authenticate: Basic}
 * header, so the browser never pops up a native login dialog (ADR 003).
 */
@Component
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		SecurityProblemDetails.write(response, SecurityProblemDetails.unauthorized(), objectMapper);
	}
}
