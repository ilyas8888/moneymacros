import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { ApiError, api, type ProblemDetails } from '../../api/client'

/**
 * Log out: end the server session, drop every cached query so nothing from this
 * user stays in memory, then go to /login. A 401 means the session was already
 * gone — still a successful log-out, so the cache is cleared and we navigate
 * regardless (handled in onSettled).
 */
export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: async () => {
      const { error, response } = await api.POST('/api/auth/logout')
      // Read the status up front (see useMe): the contract documents no error
      // responses, so the typed failure branch is narrowed away.
      const status = response.status
      if (error && status !== 401) {
        throw new ApiError(status, error as ProblemDetails | undefined)
      }
    },
    onSettled: () => {
      queryClient.clear()
      navigate('/login', { replace: true })
    },
  })
}
