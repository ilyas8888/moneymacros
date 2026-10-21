package com.moneymacros.identity.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moneymacros.IntegrationTest;
import com.moneymacros.identity.account.UserAccountRepository;
import com.moneymacros.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

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

	private ApiTestClient client;

	@BeforeEach
	void reset() {
		jdbc.execute("TRUNCATE TABLE spring_session_attributes, spring_session");
		// CASCADE because profiles references users (Day 8); it clears profiles too.
		jdbc.execute("TRUNCATE TABLE users CASCADE");
		client = new ApiTestClient(mockMvc);
	}

	@Test
	void duplicateThatBeatsThePreCheckStillGives409NotServerError() throws Exception {
		client.signup(EMAIL, PASSWORD).andExpect(status().isCreated());

		// Force the pre-check to lose the race: the row already exists, but
		// existsByEmail reports it does not, so the insert reaches the constraint.
		doReturn(false).when(accounts).existsByEmail(anyString());

		client.signup(EMAIL, PASSWORD)
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}
}
