import createClient, { type Middleware } from 'openapi-fetch'
import type { paths } from './schema'

// Methods that change state need a CSRF token; safe methods do not (ADR 003).
const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

/** Read a cookie's raw value, or undefined if it is not set. */
function readCookie(name: string): string | undefined {
  for (const part of document.cookie.split('; ')) {
    const eq = part.indexOf('=')
    if (eq > -1 && part.slice(0, eq) === name) {
      return part.slice(eq + 1)
    }
  }
  return undefined
}

/**
 * Echoes the XSRF-TOKEN cookie in the X-XSRF-TOKEN header on every state-changing
 * request. If the cookie is missing (first visit, or it expired), it asks the API
 * for a fresh one first. Spring writes the cookie value un-encoded, so it is sent
 * back verbatim (ADR 003).
 */
const csrfMiddleware: Middleware = {
  async onRequest({ request }) {
    if (!UNSAFE_METHODS.has(request.method.toUpperCase())) {
      return undefined
    }
    let token = readCookie(CSRF_COOKIE)
    if (!token) {
      await fetch('/api/auth/csrf', { credentials: 'include' })
      token = readCookie(CSRF_COOKIE)
    }
    if (token) {
      request.headers.set(CSRF_HEADER, token)
    }
    return request
  },
}

/**
 * The single typed API client, built on the generated OpenAPI types (ADR 005).
 * `credentials: 'include'` sends the session cookie; the Vite dev proxy keeps
 * everything same-origin so there is no CORS (.claude/rules/frontend.md).
 */
export const api = createClient<paths>({ credentials: 'include' })
api.use(csrfMiddleware)
