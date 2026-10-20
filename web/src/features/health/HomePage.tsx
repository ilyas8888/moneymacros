import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
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
    <div className="flex min-h-svh items-center justify-center bg-background p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <div className="flex justify-end">
            <LanguageSwitcher />
          </div>
          <CardTitle className="text-2xl">{t('common.appName')}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-col gap-1">
            <p>{t('home.greeting', { email: user?.email })}</p>
            <p className="text-sm text-muted-foreground">{apiState}</p>
          </div>
          <Button
            type="button"
            variant="outline"
            onClick={() => logout.mutate()}
            disabled={logout.isPending}
          >
            {t('home.logout')}
          </Button>
          {logout.isError && (
            <p role="alert" className="text-sm text-destructive">
              {t('home.logoutError')}
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
