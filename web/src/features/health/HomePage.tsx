import { useTranslation } from 'react-i18next'
import { LanguageSwitcher } from '../../i18n/LanguageSwitcher'
import { usePing } from './usePing'

export function HomePage() {
  const { t } = useTranslation()
  const { data, isPending, isError, error } = usePing()

  let apiState: string
  if (isPending) {
    apiState = t('home.apiChecking')
  } else if (isError) {
    apiState = t('home.apiError', { message: error.message })
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
      <p>{apiState}</p>
    </main>
  )
}
