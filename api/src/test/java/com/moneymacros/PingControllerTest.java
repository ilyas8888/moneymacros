package com.moneymacros;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

// addFilters = false: this slice is about the controller, not security. Without
// it, @WebMvcTest applies Spring Security's default (lock everything) and /api/ping
// would 401; the public list is covered end-to-end in AuthIntegrationTest instead.
@WebMvcTest(controllers = PingController.class, properties = "app.version=1.2.3")
@AutoConfigureMockMvc(addFilters = false)
class PingControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void pingReturnsOkTheTimeAndTheVersion() throws Exception {
		mockMvc.perform(get("/api/ping"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"))
				.andExpect(jsonPath("$.time").isNotEmpty())
				.andExpect(jsonPath("$.version").value("1.2.3"));
	}
}
