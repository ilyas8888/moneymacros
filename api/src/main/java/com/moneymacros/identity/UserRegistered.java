package com.moneymacros.identity;

import java.util.UUID;

/**
 * Published when a new account is created, so other modules (e.g. profile) can
 * react. Part of identity's public API; synchronous {@code @EventListener}s run
 * inside the publishing transaction (ADR 007).
 */
public record UserRegistered(UUID userId) {}
