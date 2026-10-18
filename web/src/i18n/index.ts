import i18n from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import { resources } from './translations'

/** Mirror the resolved language onto <html lang> (the detector only reads it). */
function syncHtmlLang() {
  document.documentElement.lang = i18n.resolvedLanguage ?? 'fr'
}

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    // French is the fallback; a region-only match (en-US, fr-MA) resolves to its
    // base language thanks to nonExplicitSupportedLngs.
    fallbackLng: 'fr',
    supportedLngs: ['fr', 'en'],
    nonExplicitSupportedLngs: true,
    detection: {
      // Remembered choice first, then the browser, then <html lang>. Only the
      // choice is cached (to localStorage); <html lang> is never written here.
      order: ['localStorage', 'navigator', 'htmlTag'],
      caches: ['localStorage'],
    },
    // React already escapes interpolated values.
    interpolation: { escapeValue: false },
  })

i18n.on('languageChanged', syncHtmlLang)
syncHtmlLang()

export default i18n
