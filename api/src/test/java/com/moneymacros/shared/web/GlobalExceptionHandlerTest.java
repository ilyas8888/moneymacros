package com.moneymacros.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Drives {@link GlobalExceptionHandler} through a controller that exists only
 * for this test. The controller is a static nested class registered with
 * {@code @Import}: Spring Boot's scanning skips classes nested in test classes,
 * so its endpoints never appear in a real {@code @SpringBootTest} context.
 */
@WebMvcTest
@Import({ GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ProblemController.class })
class GlobalExceptionHandlerTest {

	private static final String SECRET = "sensitive internal detail";

	@Autowired
	MockMvc mockMvc;

	@Test
	void unexpectedErrorCarriesAnErrorIdAndNoInternalMessage() throws Exception {
		String body = mockMvc.perform(get("/test/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errorId").isNotEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(body).doesNotContain(SECRET);
	}

	@Test
	void invalidBodyGives400WithFieldErrors() throws Exception {
		mockMvc.perform(post("/test/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").isNotEmpty());
	}

	@Test
	void unknownUrlGives404AsProblemDetails() throws Exception {
		mockMvc.perform(get("/no/such/endpoint"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@RestController
	static class ProblemController {

		record Payload(@NotBlank String name) {}

		@GetMapping("/test/boom")
		String boom() {
			throw new IllegalStateException(SECRET);
		}

		@PostMapping("/test/validate")
		String validate(@Valid @RequestBody Payload payload) {
			return payload.name();
		}
	}
}
