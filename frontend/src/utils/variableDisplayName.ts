import type { VariableSchema } from '@/types/template'

export function humanizeCamelCase(value: string): string {
  const spaced = value
    .replace(/\./g, ' › ')
    .replace(/_/g, ' ')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
    .trim()

  return spaced
    .split(/\s+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export function resolveVariableDisplayName(variable: Pick<VariableSchema, 'variableKey' | 'description'>): string {
  const description = variable.description?.trim()
  if (description) {
    return description
  }
  return humanizeCamelCase(variable.variableKey)
}

export function resolveFolderDisplayName(segment: string): string {
  return humanizeCamelCase(segment)
}

export function buildVariableOptionLabel(
  variable: Pick<VariableSchema, 'variableKey' | 'description'>,
): string {
  const displayName = resolveVariableDisplayName(variable)
  if (displayName === variable.variableKey) {
    return displayName
  }
  return `${displayName} (${variable.variableKey})`
}
