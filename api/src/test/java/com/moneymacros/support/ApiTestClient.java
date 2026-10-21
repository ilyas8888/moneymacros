package com.moneymacros.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * A browser-like session over {@link MockMvc} for the integration tests: it keeps
 * a cookie jar (XSRF-TOKEN and SESSION) across requests, the way a browser would,
 * and knows how to fetch a CSRF token, sign up and log in. One instance per user
 * keeps each session's cookies isolated, so a single test can drive two users.
 */
public class ApiTestClient {

	private final MockMvc mockMvc;
	private final Map<String, Cookie> jar = new HashMap<>();

	public ApiTestClient(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	/** GET the CSRF endpoint, store the XSRF-TOKEN cookie, and return its value. */
	public String ensureCsrf() throws Exception {
		perform(get("/api/auth/csrf"));
		return jar.get("XSRF-TOKEN").getValue();
	}

	public ResultActions signup(String email, String password) throws Exception {
		return perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(email, password))
				.header("X-XSRF-TOKEN", ensureCsrf()));
	}

	public ResultActions login(String email, String password) throws Exception {
		return perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(email, password))
				.header("X-XSRF-TOKEN", ensureCsrf()));
	}

	/** Attach the current cookie jar, perform the request, then capture Set-Cookie. */
	public ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
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

	private String credentials(String email, String password) {
		return """
				{"email":"%s","password":"%s"}""".formatted(email, password);
	}
}
