package com.moneymacros.identity.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moneymacros.IntegrationTest;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Proves the sign-up race ends as a clean 409 against the real database. The
 * {@code existsByEmail} pre-check is forced to miss (as it would when two signups
 * race), so the second insert genuinely hits the {@code users_email_key} unique
 * constraint in PostgreSQL. The API must answer 409, not 500.
 *
 * <p>The {@link MockitoSpyBean} overrides a bean, which gives this class its own
 * Spring context (Spring's context cache keys on bean overrides), so it cannot
 * share the one used by the other {@code @IntegrationTest}s. It is isolated to
 * this single test to keep that cost small.
 */
@IntegrationTest
@AutoConfigureMockMvc
class SignupRaceIntegrationTest {

	private static final String EMAIL = "Grad@Example.com";
	private static final String PASSWORD = "correct-horse-battery-staple";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	UserAccountRepository accounts;

	private final Map<String, Cookie> jar = new HashMap<>();

	@BeforeEach
	void reset() {
		jar.clear();
		jdbc.execute("TRUNCATE TABLE spring_session_attributes, spring_session");
		jdbc.execute("TRUNCATE TABLE users");
	}

	@Test
	void duplicateThatBeatsThePreCheckStillGives409NotServerError() throws Exception {
		signup(EMAIL, PASSWORD).andExpect(status().isCreated());

		// Force the pre-check to lose the race: the row already exists, but
		// existsByEmail reports it does not, so the insert reaches the constraint.
		doReturn(false).when(accounts).existsByEmail(anyString());

		signup(EMAIL, PASSWORD)
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	// --- helpers --------------------------------------------------------------

	private String ensureCsrf() throws Exception {
		perform(get("/api/auth/csrf")).andExpect(status().isNoContent());
		return jar.get("XSRF-TOKEN").getValue();
	}

	private ResultActions signup(String email, String password) throws Exception {
		String token = ensureCsrf();
		return perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s"}""".formatted(email, password))
				.header("X-XSRF-TOKEN", token));
	}

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
}
