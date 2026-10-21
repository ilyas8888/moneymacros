package com.moneymacros.profile.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moneymacros.identity.AuthenticatedUser;
import com.moneymacros.profile.domain.Profile;
import com.moneymacros.profile.domain.ProfileService;
import com.moneymacros.shared.FieldValidationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller slice for {@link ProfileController}: request mapping, bean
 * validation and the Problem-Detail error shapes, with the service mocked. The
 * security filters are off (addFilters = false); the authenticated principal is
 * placed in the {@link SecurityContextHolder} directly, which the
 * {@code @AuthenticationPrincipal} resolver reads on the same thread. The
 * end-to-end security (401, per-user isolation) lives in {@code ProfileIntegrationTest}.
 */
@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

	private static final AuthenticatedUser USER =
			new AuthenticatedUser(UUID.randomUUID(), "grad@example.com");
	private static final String VALID_BODY = """
			{"country":"MA","city":"Casablanca","householdSize":3,
			 "netMonthlyIncome":{"amountMinor":500000,"currency":"MAD"}}""";

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	ProfileService profileService;

	@BeforeEach
	void authenticate() {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(USER, null, List.of()));
		SecurityContextHolder.setContext(context);
	}

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getReturnsTheEmptyDefaultWhenNoRowExists() throws Exception {
		when(profileService.find(USER.id())).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.country").value("MA"))
				.andExpect(jsonPath("$.currency").value("MAD"))
				.andExpect(jsonPath("$.complete").value(false))
				.andExpect(jsonPath("$.city").doesNotExist())
				.andExpect(jsonPath("$.netMonthlyIncome").doesNotExist());
	}

	@Test
	void putSavesAndReturnsTheCompletedProfile() throws Exception {
		Profile saved = new Profile(USER.id(), "MA", "MAD");
		saved.update("MA", "MAD", "Casablanca", 3, 500000L);
		when(profileService.save(eq(USER.id()), eq("MA"), eq("Casablanca"), eq(3), eq(500000L),
				eq("MAD"))).thenReturn(saved);

		mockMvc.perform(put("/api/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.city").value("Casablanca"))
				.andExpect(jsonPath("$.householdSize").value(3))
				.andExpect(jsonPath("$.netMonthlyIncome.amountMinor").value(500000))
				.andExpect(jsonPath("$.netMonthlyIncome.currency").value("MAD"))
				.andExpect(jsonPath("$.complete").value(true));
	}

	@Test
	void putRejectsACountryOtherThanMa() throws Exception {
		mockMvc.perform(put("/api/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"country":"FR","city":"Paris","householdSize":2,
								 "netMonthlyIncome":{"amountMinor":500000,"currency":"EUR"}}"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[?(@.field == 'country')]").exists());
	}

	@Test
	void putRejectsABlankCity() throws Exception {
		mockMvc.perform(put("/api/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"country":"MA","city":"   ","householdSize":2,
								 "netMonthlyIncome":{"amountMinor":500000,"currency":"MAD"}}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field == 'city')]").exists());
	}

	@Test
	void putRejectsAMissingIncomeAmount() throws Exception {
		mockMvc.perform(put("/api/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"country":"MA","city":"Casablanca","householdSize":3,
								 "netMonthlyIncome":{"currency":"MAD"}}"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[?(@.field == 'netMonthlyIncome.amountMinor')]").exists());
	}

	@Test
	void putRejectsAnOutOfRangeHouseholdSize() throws Exception {
		mockMvc.perform(put("/api/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"country":"MA","city":"Casablanca","householdSize":13,
								 "netMonthlyIncome":{"amountMinor":500000,"currency":"MAD"}}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.field == 'householdSize')]").exists());
	}

	@Test
	void serviceRuleFailureIsReturnedAsAFieldErrorOnNetMonthlyIncome() throws Exception {
		when(profileService.save(any(), anyString(), anyString(), anyInt(), anyLong(), anyString()))
				.thenThrow(new FieldValidationException("netMonthlyIncome",
						"currency must be MAD for country MA"));

		mockMvc.perform(put("/api/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_BODY))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.errors[0].field").value("netMonthlyIncome"))
				.andExpect(jsonPath("$.errors[0].message").isNotEmpty());
	}
}
