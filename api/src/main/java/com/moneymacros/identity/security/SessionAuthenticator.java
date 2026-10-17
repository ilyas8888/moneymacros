package com.moneymacros.identity.security;

import com.moneymacros.identity.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Establishes the authenticated session after signup or login. The principal
 * stored in the session is the lightweight {@link AuthenticatedUser}, so other
 * modules read it with {@code @AuthenticationPrincipal}. The session id is
 * changed on login to defend against session fixation (ADR 003).
 */
@Component
public class SessionAuthenticator {

	private final SecurityContextHolderStrategy holderStrategy =
			SecurityContextHolder.getContextHolderStrategy();
	private final SecurityContextRepository securityContextRepository =
			new HttpSessionSecurityContextRepository();
	private final SessionAuthenticationStrategy sessionStrategy =
			new ChangeSessionIdAuthenticationStrategy();

	public void login(AuthenticatedUser principal, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication =
				UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
		// Swap the session id first: a pre-login (possibly attacker-fixed) id can
		// no longer be reused once the user is authenticated.
		sessionStrategy.onAuthentication(authentication, request, response);
		SecurityContext context = holderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		holderStrategy.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}
}
