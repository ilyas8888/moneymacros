package com.moneymacros;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal liveness endpoint for the walking skeleton. Lives in the application
 * root package because it is cross-cutting infrastructure, not part of any
 * domain module.
 */
@RestController
@Tag(name = "Ping", description = "Liveness check")
public class PingController {

	@GetMapping("/api/ping")
	@Operation(summary = "Return ok and the current server time")
	public PingResponse ping() {
		return new PingResponse("ok", Instant.now());
	}
}
