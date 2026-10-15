package com.moneymacros;

import java.time.Instant;

/**
 * Liveness payload returned by {@link PingController}. A DTO record at the API
 * edge, never an entity (see .claude/rules/backend.md).
 *
 * @param status always {@code "ok"} when the API is up
 * @param time   server time as an ISO-8601 instant
 * @param version
 */
public record PingResponse(String status, Instant time, String version) {}
