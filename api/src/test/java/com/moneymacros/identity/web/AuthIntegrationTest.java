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
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Drives the identity endpoints through the real Spring Security filter chain
 * (CSRF, session, authorization), with sessions persisted in PostgreSQL. A tiny
 * cookie jar carries the XSRF-TOKEN and SESSION cookies between requests, the way
 * a browser would.
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

	private final Map<String, Cookie> jar = new HashMap<>();

	@BeforeEach
	void reset() {
		jar.clear();
		jdbc.execute("TRUNCATE TABLE spring_session_attributes, spring_session");
		jdbc.execute("TRUNCATE TABLE users");
	}

	@Test
	void signupLogsInAndMeReturnsTheUserAndASessionRowExists() throws Exception {
		signup(EMAIL, PASSWORD)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.email").value(NORMALISED_EMAIL));

		perform(get("/api/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(NORMALISED_EMAIL));

		Integer sessions = jdbc.queryForObject("SELECT count(*) FROM spring_session", Integer.class);
		assertThat(sessions).isPositive();
	}

	@Test
	void duplicateEmailGives409AsProblemDetails() throws Exception {
		signup(EMAIL, PASSWORD).andExpect(status().isCreated());

		signup(EMAIL, PASSWORD)
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void wrongPasswordAndUnknownEmailGiveTheSame401() throws Exception {
		signup(EMAIL, PASSWORD).andExpect(status().isCreated());

		String wrongPassword = login(EMAIL, "wrong-password-xyz")
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andReturn().getResponse().getContentAsString();

		String unknownEmail = login("nobody@example.com", PASSWORD)
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(wrongPassword).isEqualTo(unknownEmail);
	}

	@Test
	void meWithoutASessionGives401AndNoBasicChallenge() throws Exception {
		perform(get("/api/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("WWW-Authenticate"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void postWithoutTheCsrfTokenGives403() throws Exception {
		perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(EMAIL, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void afterLogoutMeGives401() throws Exception {
		signup(EMAIL, PASSWORD).andExpect(status().isCreated());
		perform(get("/api/me")).andExpect(status().isOk());

		String token = ensureCsrf();
		perform(post("/api/auth/logout").header("X-XSRF-TOKEN", token))
				.andExpect(status().isNoContent());

		perform(get("/api/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void storedHashIsArgon2idAndNeverContainsThePassword() throws Exception {
		signup(EMAIL, PASSWORD).andExpect(status().isCreated());

		UserAccount account = accounts.findByEmail(NORMALISED_EMAIL).orElseThrow();
		assertThat(account.getPasswordHash()).startsWith("$argon2id$");
		assertThat(account.getPasswordHash()).doesNotContain(PASSWORD);
	}

	@Test
	void pingIsPublicThroughTheRealFilterChain() throws Exception {
		perform(get("/api/ping")).andExpect(status().isOk());
	}

	// --- helpers --------------------------------------------------------------

	/** GET the CSRF endpoint, store the XSRF-TOKEN cookie, and return its value. */
	private String ensureCsrf() throws Exception {
		perform(get("/api/auth/csrf")).andExpect(status().isNoContent());
		return jar.get("XSRF-TOKEN").getValue();
	}

	private ResultActions signup(String email, String password) throws Exception {
		String token = ensureCsrf();
		return perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(email, password))
				.header("X-XSRF-TOKEN", token));
	}

	private ResultActions login(String email, String password) throws Exception {
		String token = ensureCsrf();
		return perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(email, password))
				.header("X-XSRF-TOKEN", token));
	}

	/** Attach the current cookie jar, perform the request, then capture Set-Cookie. */
	private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
		jar.values().forEach(builder::cookie);
		return mockMvc.perform(builder).andDo(result -> {
			for (Cookie cookie : result.getResponse().getCookies()) {
				if (cookie.getMaxAge() == 0) {
					jar.remove(cookie.getName());
				} else {
					jar.put(cookie.getName(), cookie);
				}
			}
		});
	}

	private String body(String email, String password) {
		return """
				{"email":"%s","password":"%s"}""".formatted(email, password);
	}
}
