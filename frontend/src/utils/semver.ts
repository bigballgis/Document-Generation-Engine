export interface SemverParts {
  major: number
  minor: number
  patch: number
}

const SEMVER_PATTERN = /^\d+\.\d+\.\d+$/

export function isValidSemver(value: string): boolean {
  return SEMVER_PATTERN.test(value.trim())
}

export function parseSemver(value: string): SemverParts | null {
  if (!isValidSemver(value)) {
    return null
  }
  const [major, minor, patch] = value.trim().split('.').map(Number)
  return { major, minor, patch }
}

export function formatSemver(parts: SemverParts): string {
  return `${parts.major}.${parts.minor}.${parts.patch}`
}

export function bumpMajor(current: SemverParts): SemverParts {
  return { major: current.major + 1, minor: 0, patch: 0 }
}

export function bumpMinor(current: SemverParts): SemverParts {
  return { major: current.major, minor: current.minor + 1, patch: 0 }
}

export function bumpPatch(current: SemverParts): SemverParts {
  return { major: current.major, minor: current.minor, patch: current.patch + 1 }
}

export type SemverBumpLevel = 'major' | 'minor' | 'patch'

export function suggestNextVersions(baseVersion: string | null): Record<SemverBumpLevel, string> {
  const parsed = baseVersion ? parseSemver(baseVersion) : null
  if (!parsed) {
    const initial = '1.0.0'
    return { major: initial, minor: initial, patch: initial }
  }
  return {
    major: formatSemver(bumpMajor(parsed)),
    minor: formatSemver(bumpMinor(parsed)),
    patch: formatSemver(bumpPatch(parsed)),
  }
}

export function conflictsWithExisting(
  candidate: string,
  existingVersions: string[],
): boolean {
  if (!isValidSemver(candidate)) {
    return true
  }
  return existingVersions.some((version) => version.trim() === candidate.trim())
}
