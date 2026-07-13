/**
 * Local structured-content draft storage (LR-C2).
 *
 * Key format (C2-C3): `docgen.structuredDraft.v1:{userId}:{templateId}:{devVersionId}`
 * Payload is a structure snapshot only — no undo history (C3 storage separation).
 */

const STRUCTURED_DRAFT_KEY_PREFIX = 'docgen.structuredDraft.v1:'

const STRUCTURED_DRAFT_SCHEMA_VERSION = 1 as const

export interface StructuredContentDraftPayload {
  schemaVersion: typeof STRUCTURED_DRAFT_SCHEMA_VERSION
  structureJson: string
  draftUpdatedAt: string
  serverUpdatedAt?: string | null
  /** Optional binding-anchor disambiguation within the same template+devVersion key. */
  anchorId?: string | null
}

export function buildStructuredDraftStorageKey(
  userId: string,
  templateId: string,
  devVersionId: string,
): string {
  return `${STRUCTURED_DRAFT_KEY_PREFIX}${userId}:${templateId}:${devVersionId}`
}

function isStructuredDraftStorageKey(key: string): boolean {
  return key.startsWith(STRUCTURED_DRAFT_KEY_PREFIX)
}

function parseStructuredDraftPayload(raw: string): StructuredContentDraftPayload | null {
  try {
    const parsed = JSON.parse(raw) as Partial<StructuredContentDraftPayload>
    if (
      parsed.schemaVersion !== STRUCTURED_DRAFT_SCHEMA_VERSION ||
      typeof parsed.structureJson !== 'string' ||
      typeof parsed.draftUpdatedAt !== 'string'
    ) {
      return null
    }
    return {
      schemaVersion: STRUCTURED_DRAFT_SCHEMA_VERSION,
      structureJson: parsed.structureJson,
      draftUpdatedAt: parsed.draftUpdatedAt,
      serverUpdatedAt: parsed.serverUpdatedAt ?? null,
      anchorId: parsed.anchorId ?? null,
    }
  } catch {
    return null
  }
}

export function readStructuredDraft(
  storage: Storage,
  key: string,
): StructuredContentDraftPayload | null {
  try {
    const raw = storage.getItem(key)
    if (raw == null) {
      return null
    }
    const payload = parseStructuredDraftPayload(raw)
    if (!payload) {
      storage.removeItem(key)
      return null
    }
    return payload
  } catch {
    return null
  }
}

function listDraftEntries(
  storage: Storage,
): Array<{ key: string; draftUpdatedAt: string }> {
  const entries: Array<{ key: string; draftUpdatedAt: string }> = []
  for (let index = 0; index < storage.length; index += 1) {
    const key = storage.key(index)
    if (!key || !isStructuredDraftStorageKey(key)) {
      continue
    }
    const payload = readStructuredDraft(storage, key)
    if (!payload) {
      continue
    }
    entries.push({ key, draftUpdatedAt: payload.draftUpdatedAt })
  }
  return entries
}

function isQuotaExceededError(error: unknown): boolean {
  if (!error || typeof error !== 'object') {
    return false
  }
  const name = 'name' in error ? String((error as { name?: unknown }).name) : ''
  const code = 'code' in error ? Number((error as { code?: unknown }).code) : NaN
  return name === 'QuotaExceededError' || name === 'NS_ERROR_DOM_QUOTA_REACHED' || code === 22
}

/**
 * Writes a draft with quota eviction: on QuotaExceededError, drop oldest other
 * `docgen.structuredDraft.v1:*` entries by `draftUpdatedAt` and retry.
 * Returns false when the write could not be completed (silent skip).
 */
export function writeStructuredDraft(
  storage: Storage,
  key: string,
  payload: StructuredContentDraftPayload,
): boolean {
  const serialized = JSON.stringify(payload)

  try {
    storage.setItem(key, serialized)
    return true
  } catch (error) {
    if (!isQuotaExceededError(error)) {
      return false
    }
  }

  // Quota path: evict oldest other drafts ascending by draftUpdatedAt.
  const others = listDraftEntries(storage)
    .filter((entry) => entry.key !== key)
    .sort((a, b) => a.draftUpdatedAt.localeCompare(b.draftUpdatedAt))

  for (const entry of others) {
    try {
      storage.removeItem(entry.key)
    } catch {
      // ignore removal failures
    }
    try {
      storage.setItem(key, serialized)
      return true
    } catch (error) {
      if (!isQuotaExceededError(error)) {
        return false
      }
    }
  }

  return false
}

export function clearStructuredDraft(storage: Storage, key: string): void {
  try {
    storage.removeItem(key)
  } catch {
    // ignore
  }
}

/**
 * C2-C9 / BDD-LRP-C2-004/005: clear **only** the exact draft key for the current
 * userId+templateId+devVersionId. Never sweep by templateId (would wipe other
 * users / other devVersionIds). No-op when any scope part is missing — do not
 * fall back to a templateId scan.
 */
export function clearExactStructuredDraftOnSave(
  storage: Storage,
  userId: string | null | undefined,
  templateId: string | null | undefined,
  devVersionId: string | null | undefined,
): void {
  if (!userId || !templateId || !devVersionId) {
    return
  }
  clearStructuredDraft(storage, buildStructuredDraftStorageKey(userId, templateId, devVersionId))
}

export function shouldOfferDraftRecovery(
  draft: StructuredContentDraftPayload | null,
  serverStructureJson: string,
  currentAnchorId?: string | null,
): boolean {
  if (!draft) {
    return false
  }
  if (draft.structureJson === serverStructureJson) {
    return false
  }
  if (draft.anchorId && currentAnchorId && draft.anchorId !== currentAnchorId) {
    return false
  }
  return true
}
