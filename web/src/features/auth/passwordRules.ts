// Mirrors SignupRequest's @Size(min = 12, max = 128)
// (api/src/main/java/com/moneymacros/identity/web/SignupRequest.java). Kept by
// hand because numeric constraints aren't readable at runtime from the
// generated types (ADR 005).
export const PASSWORD_MIN = 12
export const PASSWORD_MAX = 128
