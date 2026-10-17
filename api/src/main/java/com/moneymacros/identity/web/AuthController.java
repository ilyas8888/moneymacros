package com.moneymacros.identity.web;

import com.moneymacros.identity.AuthenticatedUser;
import com.moneymacros.identity.account.AccountService;
import com.moneymacros.identity.account.LoginService;
import com.moneymacros.identity.security.SessionAuthenticator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sign up, log in, log out, and hand the SPA a fresh CSRF cookie (ADR 003). */
@RestController
@Tag(name = "Auth", description = "Sign up, log in and log out")
class AuthController {

	private final AccountService accountService;
	private final LoginService loginService;
	private final SessionAuthenticator sessionAuthenticator;

	AuthController(AccountService accountService, LoginService loginService,
			SessionAuthenticator sessionAuthenticator) {
		this.accountService = accountService;
		this.loginService = loginService;
		this.sessionAuthenticator = sessionAuthenticator;
	}

	@PostMapping("/api/auth/signup")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an account, then log in")
	UserResponse signup(@Valid @RequestBody SignupRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		AuthenticatedUser user = accountService.register(request.email(), request.password());
		sessionAuthenticator.login(user, httpRequest, httpResponse);
		return UserResponse.of(user);
	}

	@PostMapping("/api/auth/login")
	@Operation(summary = "Log in with email and password")
	UserResponse login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		AuthenticatedUser user = loginService.authenticate(request.email(), request.password());
		sessionAuthenticator.login(user, httpRequest, httpResponse);
		return UserResponse.of(user);
	}

	@PostMapping("/api/auth/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Invalidate the current session")
	void logout(HttpServletRequest httpRequest) {
		HttpSession session = httpRequest.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
	}

	@GetMapping("/api/auth/csrf")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Ensure a fresh XSRF-TOKEN cookie is set")
	void csrf(CsrfToken token) {
		// Reading the token materialises the deferred token so the XSRF-TOKEN
		// cookie is written on the response.
		token.getToken();
	}
}
