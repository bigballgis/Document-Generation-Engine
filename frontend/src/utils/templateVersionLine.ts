import type { TemplateVersionLineSummary } from '@/types/template'

export function isInFlightVersionLine(row: TemplateVersionLineSummary): boolean {
  return row.releaseVersion == null || row.releaseVersion.trim() === ''
}

export function versionLineDisplayLabel(
  t: (key: string, params?: Record<string, unknown>) => string,
  row: TemplateVersionLineSummary,
): string {
  if (isInFlightVersionLine(row)) {
    return t('templates.versionLines.inFlightLabel', { number: row.devVersionNumber })
  }
  return t('templates.versionLines.releaseLabel', {
    version: row.releaseVersion,
    number: row.devVersionNumber,
  })
}
