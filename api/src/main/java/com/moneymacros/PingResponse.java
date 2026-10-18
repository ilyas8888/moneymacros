package com.moneymacros;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;

/**
 * Liveness payload returned by {@link PingController}. A DTO record at the API
 * edge, never an entity (see .claude/rules/backend.md).
 *
 * @param status always {@code "ok"} when the API is up
 * @param time   server time as an ISO-8601 instant
 * @param version the API version, from pom.xml
 */
public record PingResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) String status,
		@Schema(requiredMode = RequiredMode.REQUIRED) Instant time,
		@Schema(requiredMode = RequiredMode.REQUIRED) String version) {}
