import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'

const LANGUAGES = ['fr', 'en'] as const

/** FR | EN switch shown on every page; the choice is remembered (localStorage). */
export function LanguageSwitcher() {
  const { t, i18n } = useTranslation()
  const current = i18n.resolvedLanguage

  return (
    <nav aria-label={t('language.switchLabel')} className="flex gap-1">
      {LANGUAGES.map((lng) => {
        const active = current === lng
        return (
          <Button
            key={lng}
            type="button"
            size="sm"
            // Active language stays filled (secondary) and pressed, not
            // disabled, so the current choice reads as selected, not faded.
            variant={active ? 'secondary' : 'ghost'}
            onClick={() => void i18n.changeLanguage(lng)}
            aria-pressed={active}
          >
            {t(`language.${lng}`)}
          </Button>
        )
      })}
    </nav>
  )
}
