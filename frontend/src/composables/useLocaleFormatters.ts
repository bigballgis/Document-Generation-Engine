import { useI18n } from 'vue-i18n'

export function useLocaleFormatters() {
  const { locale } = useI18n()

  function formatDateTime(value: string | Date): string {
    const date = value instanceof Date ? value : new Date(value)
    // FOS-W5-2: management timestamps render in UTC with an explicit zone marker
    // (matches form labels that already say UTC).
    return date.toLocaleString(locale.value, {
      timeZone: 'UTC',
      timeZoneName: 'short',
    })
  }

  function formatNumber(value: number): string {
    return value.toLocaleString(locale.value)
  }

  return {
    formatDateTime,
    formatNumber,
  }
}
