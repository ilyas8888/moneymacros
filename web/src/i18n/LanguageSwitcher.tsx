import { useTranslation } from 'react-i18next'

const LANGUAGES = ['fr', 'en'] as const

/** FR | EN switch shown on every page; the choice is remembered (localStorage). */
export function LanguageSwitcher() {
  const { t, i18n } = useTranslation()
  const current = i18n.resolvedLanguage

  return (
    <nav aria-label={t('language.switchLabel')} className="language-switcher">
      {LANGUAGES.map((lng) => (
        <button
          key={lng}
          type="button"
          onClick={() => void i18n.changeLanguage(lng)}
          aria-pressed={current === lng}
          disabled={current === lng}
        >
          {t(`language.${lng}`)}
        </button>
      ))}
    </nav>
  )
}
