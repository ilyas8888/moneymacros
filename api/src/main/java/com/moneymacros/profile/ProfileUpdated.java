package com.moneymacros.profile;

import com.moneymacros.shared.Money;
import java.util.UUID;

/**
 * Published when a user saves their profile, carrying the net monthly income
 * that planning needs to recalculate targets. Part of profile's public API;
 * synchronous {@code @EventListener}s run inside the publishing transaction
 * (ADR 007).
 */
public record ProfileUpdated(UUID userId, Money netMonthlyIncome) {}
