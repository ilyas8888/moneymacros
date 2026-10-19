import { useTranslation } from 'react-i18next'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useMe } from './useMe'

/** Protects routes: a logged-out user is sent to /login, remembering where they
 *  were headed so login can send them back. */
export function RequireAuth() {
  const { t } = useTranslation()
  const { data: user, isPending } = useMe()
  const location = useLocation()

  if (isPending) {
    return <p>{t('common.loading')}</p>
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}

/** Keeps logged-in users off /login and /signup by sending them to the home page. */
export function RedirectIfAuthed() {
  const { t } = useTranslation()
  const { data: user, isPending } = useMe()

  if (isPending) {
    return <p>{t('common.loading')}</p>
  }
  if (user) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
