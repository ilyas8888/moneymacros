package com.moneymacros.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moneymacros.IntegrationTest;
import com.moneymacros.shared.Money;
import com.moneymacros.support.ApiTestClient;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end profile behaviour through the real Spring Security filter chain, with
 * sessions and rows in PostgreSQL. Two users (A and B) prove the isolation
 * guarantee: each one only ever sees and changes their own profile.
 */
@IntegrationTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ProfileIntegrationTest {

	private static final String PASSWORD = "correct-horse-battery-staple";
	private static final String CASABLANCA = """
			{"country":"MA","city":"Casablanca","householdSize":3,
			 "netMonthlyIncome":{"amountMinor":500000,"currency":"MAD"}}""";
	private static final String RABAT = """
			{"country":"MA","city":"Rabat","householdSize":1,
			 "netMonthlyIncome":{"amountMinor":300000,"currency":"MAD"}}""";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ApplicationEvents applicationEvents;

	@BeforeEach
	void reset() {
		jdbc.execute("TRUNCATE TABLE spring_session_attributes, spring_session");
		jdbc.execute("TRUNCATE TABLE users CASCADE");
	}

	@Test
	void signupCreatesAnEmptyMoroccanProfileRow() throws Exception {
		UUID userId = signup("a@example.com");

		Map<String, Object> row = jdbc.queryForMap(
				"SELECT country, currency, city, household_size, net_income_minor "
						+ "FROM profiles WHERE user_id = ?", userId);
		assertThat(((String) row.get("country")).trim()).isEqualTo("MA");
		assertThat(((String) row.get("currency")).trim()).isEqualTo("MAD");
		assertThat(row.get("city")).isNull();
		assertThat(row.get("household_size")).isNull();
		assertThat(row.get("net_income_minor")).isNull();
	}

	@Test
	void getReturnsTheEmptyProfileThenPutCompletesIt() throws Exception {
		ApiTestClient a = new ApiTestClient(mockMvc);
		a.signup("a@example.com", PASSWORD).andExpect(status().isCreated());

		a.perform(get("/api/profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.country").value("MA"))
				.andExpect(jsonPath("$.complete").value(false))
				.andExpect(jsonPath("$.city").doesNotExist());

		putProfile(a, CASABLANCA).andExpect(status().isOk());

		a.perform(get("/api/profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.city").value("Casablanca"))
				.andExpect(jsonPath("$.householdSize").value(3))
				.andExpect(jsonPath("$.netMonthlyIncome.amountMinor").value(500000))
				.andExpect(jsonPath("$.netMonthlyIncome.currency").value("MAD"))
				.andExpect(jsonPath("$.complete").value(true));
	}

	@Test
	void eachUserOnlySeesAndChangesTheirOwnProfile() throws Exception {
		ApiTestClient a = new ApiTestClient(mockMvc);
		ApiTestClient b = new ApiTestClient(mockMvc);
		a.signup("a@example.com", PASSWORD).andExpect(status().isCreated());
		b.signup("b@example.com", PASSWORD).andExpect(status().isCreated());

		putProfile(a, CASABLANCA).andExpect(status().isOk());
		putProfile(b, RABAT).andExpect(status().isOk());

		a.perform(get("/api/profile")).andExpect(jsonPath("$.city").value("Casablanca"));
		b.perform(get("/api/profile")).andExpect(jsonPath("$.city").value("Rabat"));
	}

	@Test
	void anotherUsersIdInThePutBodyIsIgnored() throws Exception {
		ApiTestClient a = new ApiTestClient(mockMvc);
		ApiTestClient b = new ApiTestClient(mockMvc);
		UUID bId = signup(b, "b@example.com");
		a.signup("a@example.com", PASSWORD).andExpect(status().isCreated());

		// A sends a body that sneaks in B's id and a random profile id; both ignored.
		putProfile(a, """
				{"id":"%s","userId":"%s","country":"MA","city":"Casablanca","householdSize":3,
				 "netMonthlyIncome":{"amountMinor":500000,"currency":"MAD"}}"""
				.formatted(UUID.randomUUID(), bId))
				.andExpect(status().isOk());

		// A's own profile changed; B's is untouched (still empty).
		a.perform(get("/api/profile")).andExpect(jsonPath("$.city").value("Casablanca"));
		b.perform(get("/api/profile")).andExpect(jsonPath("$.complete").value(false));
	}

	@Test
	void profileUpdatedIsPublishedWithTheRightPayload() throws Exception {
		ApiTestClient a = new ApiTestClient(mockMvc);
		UUID userId = signup(a, "a@example.com");

		putProfile(a, CASABLANCA).andExpect(status().isOk());

		assertThat(applicationEvents.stream(ProfileUpdated.class))
				.singleElement()
				.satisfies(event -> {
					assertThat(event.userId()).isEqualTo(userId);
					assertThat(event.netMonthlyIncome())
							.isEqualTo(new Money(500000L, Currency.getInstance("MAD")));
				});
	}

	@Test
	void putRejectsACurrencyThatDoesNotMatchTheCountry() throws Exception {
		ApiTestClient a = new ApiTestClient(mockMvc);
		a.signup("a@example.com", PASSWORD).andExpect(status().isCreated());

		putProfile(a, """
				{"country":"MA","city":"Casablanca","householdSize":3,
				 "netMonthlyIncome":{"amountMinor":500000,"currency":"EUR"}}""")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[0].field").value("netMonthlyIncome"));
	}

	@Test
	void putRejectsIncomeAboveTheCap() throws Exception {
		ApiTestClient a = new ApiTestClient(mockMvc);
		a.signup("a@example.com", PASSWORD).andExpect(status().isCreated());

		putProfile(a, """
				{"country":"MA","city":"Casablanca","householdSize":3,
				 "netMonthlyIncome":{"amountMinor":2000000000,"currency":"MAD"}}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("netMonthlyIncome"));
	}

	@Test
	void profileWithoutASessionGives401() throws Exception {
		new ApiTestClient(mockMvc).perform(get("/api/profile"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	// --- helpers --------------------------------------------------------------

	private ResultActions putProfile(ApiTestClient client, String body) throws Exception {
		return client.perform(put("/api/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.header("X-XSRF-TOKEN", client.ensureCsrf()));
	}

	/** Sign up on a fresh client and return the new user's id. */
	private UUID signup(String email) throws Exception {
		return signup(new ApiTestClient(mockMvc), email);
	}

	private UUID signup(ApiTestClient client, String email) throws Exception {
		String response = client.signup(email, PASSWORD)
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(response, "$.id"));
	}
}
