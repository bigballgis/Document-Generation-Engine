import type { MasterRevisionLineLabel } from '@/types/master'

type RevisionLineTranslate = (key: string, values?: Record<string, unknown>) => string

export function formatMasterRevisionLineLabel(
  t: RevisionLineTranslate,
  lineLabel: MasterRevisionLineLabel | string | undefined,
  revisionSequence?: number,
): string {
  if (revisionSequence != null) {
    return t('masters.revisionLines.revisionSequence', { sequence: revisionSequence })
  }
  if (lineLabel === 'CURRENT') {
    return t('masters.revisionLines.currentLine')
  }
  if (lineLabel === 'HISTORICAL') {
    return t('masters.revisionLines.historicalLine')
  }
  return lineLabel ?? ''
}
