package com.moneymacros;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PingController.class, properties = "app.version=1.2.3")
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
