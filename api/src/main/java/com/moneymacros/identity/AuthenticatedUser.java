package com.moneymacros.identity;

import java.io.Serializable;
import java.util.UUID;

/**
 * The authenticated user as other modules see it: the security principal stored
 * in the session. {@code Serializable} because Spring Session persists it. Read
 * it in controllers with {@code @AuthenticationPrincipal AuthenticatedUser}.
 */
public record AuthenticatedUser(UUID id, String email) implements Serializable {}
