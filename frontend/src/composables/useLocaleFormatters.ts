import { useI18n } from 'vue-i18n'

export function useLocaleFormatters() {
  const { locale } = useI18n()

  function formatDateTime(value: string | Date): string {
    const date = value instanceof Date ? value : new Date(value)
    return date.toLocaleString(locale.value)
  }

  function formatNumber(value: number): string {
    return value.toLocaleString(locale.value)
  }

  return {
    formatDateTime,
    formatNumber,
  }
}
