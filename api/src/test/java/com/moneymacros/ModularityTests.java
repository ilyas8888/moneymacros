package com.moneymacros;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies the Spring Modulith structure: every module talks to others only
 * through their public API or published events (ADR 001).
 */
class ModularityTests {

	static final ApplicationModules modules = ApplicationModules.of(ApiApplication.class);

	@Test
	void verifiesModuleBoundaries() {
		modules.verify();
	}
}
