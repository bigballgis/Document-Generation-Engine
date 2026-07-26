/**
 * Convert an Element Plus datetime-picker wall-clock string to true UTC ISO.
 *
 * Pickers must use `value-format="YYYY-MM-DDTHH:mm:ss"` (no literal Z). A trailing
 * `Z` from the legacy lying format is stripped and the digits are still treated
 * as local wall-clock before conversion.
 */
export function localWallClockToUtcIso(value: string | null | undefined): string | null {
  if (value == null) {
    return null
  }
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }

  const cleaned = trimmed.replace(/[Zz]$/, '')
  const match = cleaned.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})$/)
  if (!match) {
    const parsed = new Date(trimmed)
    return Number.isNaN(parsed.getTime()) ? null : parsed.toISOString()
  }

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const hour = Number(match[4])
  const minute = Number(match[5])
  const second = Number(match[6])
  const local = new Date(year, month - 1, day, hour, minute, second)
  if (Number.isNaN(local.getTime())) {
    return null
  }
  return local.toISOString()
}

export function localWallClockToUtcIsoOrEmpty(value: string | null | undefined): string {
  return localWallClockToUtcIso(value) ?? ''
}
