import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useLogout } from '../auth/useLogout'
import { useMe } from '../auth/useMe'
import { LanguageSwitcher } from '../../i18n/LanguageSwitcher'
import { usePing } from './usePing'

export function HomePage() {
  const { t } = useTranslation()
  const { data: user } = useMe()
  const logout = useLogout()
  const { data, isPending, isError, error } = usePing()

  // Keep the English error detail out of the UI; log it for debugging instead.
  useEffect(() => {
    if (isError) {
      console.error('Failed to load API status', error)
    }
  }, [isError, error])

  let apiState: string
  if (isPending) {
    apiState = t('home.apiChecking')
  } else if (isError) {
    apiState = t('home.apiError')
  } else {
    apiState = t('home.apiStatus', {
      status: data.status,
      version: data.version,
    })
  }

  return (
    <main>
      <LanguageSwitcher />
      <h1>{t('common.appName')}</h1>
      <p>{t('home.greeting', { email: user?.email })}</p>
      <p>{apiState}</p>
      <button
        type="button"
        onClick={() => logout.mutate()}
        disabled={logout.isPending}
      >
        {t('home.logout')}
      </button>
      {logout.isError && <p role="alert">{t('home.logoutError')}</p>}
    </main>
  )
}
