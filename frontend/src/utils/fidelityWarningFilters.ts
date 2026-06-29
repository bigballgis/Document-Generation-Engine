import type { FidelityWarning } from '@/types/template'

export interface FidelityWarningFilters {
  warningCode: string
  location: string
  artifact: string
  viewed: 'all' | 'viewed' | 'unviewed'
}

export const DEFAULT_FIDELITY_WARNING_FILTERS: FidelityWarningFilters = {
  warningCode: '',
  location: '',
  artifact: '',
  viewed: 'all',
}

export function filterFidelityWarnings(
  warnings: FidelityWarning[],
  filters: FidelityWarningFilters,
): FidelityWarning[] {
  return warnings.filter((warning) => {
    if (filters.warningCode && !warning.code.toLowerCase().includes(filters.warningCode.toLowerCase())) {
      return false
    }
    const location = warning.location ?? ''
    if (filters.location && !location.toLowerCase().includes(filters.location.toLowerCase())) {
      return false
    }
    const artifact = warning.artifact ?? ''
    if (filters.artifact && !artifact.toLowerCase().includes(filters.artifact.toLowerCase())) {
      return false
    }
    if (filters.viewed === 'viewed' && !warning.viewed) {
      return false
    }
    if (filters.viewed === 'unviewed' && warning.viewed) {
      return false
    }
    return true
  })
}

export function uniqueWarningCodes(warnings: FidelityWarning[]): string[] {
  return [...new Set(warnings.map((warning) => warning.code))].sort()
}

export function uniqueArtifacts(warnings: FidelityWarning[]): string[] {
  return [...new Set(warnings.map((warning) => warning.artifact).filter(Boolean) as string[])].sort()
}
