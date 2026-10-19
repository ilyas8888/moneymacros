import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { ApiError, api, type ProblemDetails } from '../../api/client'

/**
 * Log out: end the server session, drop every cached query so nothing from this
 * user stays in memory, then go to /login. A 401 means the session was already
 * gone — still a successful log-out. Any other failure (403, 500, network) means
 * the server session may still be alive, so we must NOT fake a log-out: the
 * mutation errors and the caller stays put and shows a message.
 */
export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: async () => {
      const { error, response } = await api.POST('/api/auth/logout')
      const { ok, status } = response
      // Decide from the HTTP status, never from whether openapi-fetch parsed an
      // error body: when the API is down the Vite proxy answers 500 with an
      // empty body, so there is no parsed error and a real failure would
      // otherwise pass as success. A 401 means the session was already gone —
      // still a successful log-out.
      if (!ok && status !== 401) {
        throw new ApiError(status, error as ProblemDetails | undefined)
      }
    },
    onSuccess: () => {
      // Only here, once the session is actually gone. Record "logged out"
      // explicitly (set ['me'] to null) rather than leaving the guards to
      // re-derive it from a follow-up /api/me fetch, so RequireAuth and
      // RedirectIfAuthed see a definite logged-out state right away and the
      // navigation to /login is never bounced back to /.
      queryClient.clear()
      queryClient.setQueryData(['me'], null)
      navigate('/login', { replace: true })
    },
  })
}
