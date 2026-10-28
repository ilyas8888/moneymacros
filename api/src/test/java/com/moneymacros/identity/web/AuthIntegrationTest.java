package com.moneymacros.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moneymacros.IntegrationTest;
import com.moneymacros.identity.account.UserAccount;
import com.moneymacros.identity.account.UserAccountRepository;
import com.moneymacros.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Drives the identity endpoints through the real Spring Security filter chain
 * (CSRF, session, authorization), with sessions persisted in PostgreSQL. The
 * {@link ApiTestClient} carries the XSRF-TOKEN and SESSION cookies between
 * requests, the way a browser would.
 */
@IntegrationTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

	private static final String EMAIL = "Grad@Example.com";
	private static final String NORMALISED_EMAIL = "grad@example.com";
	private static final String PASSWORD = "correct-horse-battery-staple";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserAccountRepository accounts;

	@Autowired
	JdbcTemplate jdbc;

	private ApiTestClient client;

	@BeforeEach
	void reset() {
		jdbc.execute("TRUNCATE TABLE spring_session_attributes, spring_session");
		// CASCADE because profiles references users (Day 8); it clears profiles too.
		jdbc.execute("TRUNCATE TABLE users CASCADE");
		client = new ApiTestClient(mockMvc);
	}

	@Test
	void signupLogsInAndMeReturnsTheUserAndASessionRowExists() throws Exception {
		client.signup(EMAIL, PASSWORD)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.email").value(NORMALISED_EMAIL));

		client.perform(get("/api/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(NORMALISED_EMAIL));

		Integer sessions = jdbc.queryForObject("SELECT count(*) FROM spring_session", Integer.class);
		assertThat(sessions).isPositive();
	}

	@Test
	void duplicateEmailGives409AsProblemDetails() throws Exception {
		client.signup(EMAIL, PASSWORD).andExpect(status().isCreated());

		client.signup(EMAIL, PASSWORD)
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void wrongPasswordAndUnknownEmailGiveTheSame401() throws Exception {
		client.signup(EMAIL, PASSWORD).andExpect(status().isCreated());

		String wrongPassword = client.login(EMAIL, "wrong-password-xyz")
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andReturn().getResponse().getContentAsString();

		String unknownEmail = client.login("nobody@example.com", PASSWORD)
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(wrongPassword).isEqualTo(unknownEmail);
	}

	@Test
	void meWithoutASessionGives401AndNoBasicChallenge() throws Exception {
		client.perform(get("/api/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("WWW-Authenticate"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void postWithoutTheCsrfTokenGives403() throws Exception {
		client.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s"}""".formatted(EMAIL, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void afterLogoutMeGives401() throws Exception {
		client.signup(EMAIL, PASSWORD).andExpect(status().isCreated());
		client.perform(get("/api/me")).andExpect(status().isOk());

		String token = client.ensureCsrf();
		client.perform(post("/api/auth/logout").header("X-XSRF-TOKEN", token))
				.andExpect(status().isNoContent());

		client.perform(get("/api/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void storedHashIsArgon2idAndNeverContainsThePassword() throws Exception {
		client.signup(EMAIL, PASSWORD).andExpect(status().isCreated());

		UserAccount account = accounts.findByEmail(NORMALISED_EMAIL).orElseThrow();
		assertThat(account.getPasswordHash()).startsWith("$argon2id$");
		assertThat(account.getPasswordHash()).doesNotContain(PASSWORD);
	}

	@Test
	void pingIsPublicThroughTheRealFilterChain() throws Exception {
		client.perform(get("/api/ping")).andExpect(status().isOk());
	}

	@Test
	void swaggerUiHtmlIsNotBlockedByTheFilterChain() throws Exception {
		// The UI is disabled outside dev, so the path 404s rather than redirecting;
		// what matters is that security no longer answers 401 for it.
		int status = client.perform(get("/swagger-ui.html"))
				.andReturn().getResponse().getStatus();
		assertThat(status).isNotEqualTo(401);
	}
}
