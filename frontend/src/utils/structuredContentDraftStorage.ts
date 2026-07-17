/**
 * Local structured-content draft storage (LR-C2 + CE-U21).
 *
 * Key formats:
 * - Triple (no anchor / CM mounts): `docgen.structuredDraft.v1:{userId}:{templateId}:{devVersionId}`
 * - Four-tuple (binding editor with anchorId):
 *   `docgen.structuredDraft.v1:{userId}:{templateId}:{devVersionId}:{anchorId}`
 *
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
  anchorId?: string | null,
): string {
  const base = `${STRUCTURED_DRAFT_KEY_PREFIX}${userId}:${templateId}:${devVersionId}`
  if (anchorId) {
    return `${base}:${anchorId}`
  }
  return base
}

/**
 * Legacy LR-C2 triple key (no anchor segment). Used for one-time migration (U21-D4).
 */
export function buildLegacyStructuredDraftStorageKey(
  userId: string,
  templateId: string,
  devVersionId: string,
): string {
  return buildStructuredDraftStorageKey(userId, templateId, devVersionId)
}

/** True when a legacy payload may be claimed by the given anchor (U21-D4). */
export function isLegacyDraftClaimableByAnchor(
  draft: StructuredContentDraftPayload | null,
  currentAnchorId: string | null | undefined,
): boolean {
  if (!draft || !currentAnchorId) {
    return false
  }
  if (!draft.anchorId) {
    return true
  }
  return draft.anchorId === currentAnchorId
}

function isStructuredDraftStorageKey(key: string): boolean {
  return key.startsWith(STRUCTURED_DRAFT_KEY_PREFIX)
}

function parseStructuredDraftPayload(raw: string): StructuredContentDraftPayload | null {
  try {
    const parsed = JSON.parse(raw) as Partial<StructuredContentDraftPayload>
    if (
      parsed.schemaVersion !== STRUCTURED_DRAFT_SCHEMA_VERSION
      || typeof parsed.structureJson !== 'string'
      || typeof parsed.draftUpdatedAt !== 'string'
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

/**
 * Read authoritative per-anchor draft, falling back to claimable legacy triple key (U21-D4).
 */
export function readStructuredDraftForAnchor(
  storage: Storage,
  userId: string,
  templateId: string,
  devVersionId: string,
  anchorId: string | null | undefined,
): StructuredContentDraftPayload | null {
  const primaryKey = buildStructuredDraftStorageKey(userId, templateId, devVersionId, anchorId)
  const primary = readStructuredDraft(storage, primaryKey)
  if (primary) {
    return primary
  }
  if (!anchorId) {
    return null
  }
  const legacyKey = buildLegacyStructuredDraftStorageKey(userId, templateId, devVersionId)
  const legacy = readStructuredDraft(storage, legacyKey)
  if (!isLegacyDraftClaimableByAnchor(legacy, anchorId)) {
    return null
  }
  return legacy
}

/**
 * Delete legacy triple key when it is claimable by the current anchor (U21-D4 migration).
 */
export function clearClaimableLegacyStructuredDraft(
  storage: Storage,
  userId: string,
  templateId: string,
  devVersionId: string,
  anchorId: string | null | undefined,
): void {
  if (!anchorId) {
    return
  }
  const legacyKey = buildLegacyStructuredDraftStorageKey(userId, templateId, devVersionId)
  const legacy = readStructuredDraft(storage, legacyKey)
  if (!isLegacyDraftClaimableByAnchor(legacy, anchorId)) {
    return
  }
  clearStructuredDraft(storage, legacyKey)
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
 * C2-C9 / BDD-LRP-C2-004/005 / CE-U21-DAC-005: clear **only** the exact draft key for the
 * current userId+templateId+devVersionId(+optional anchorId). Never sweep by templateId.
 * When `anchorId` is set, also removes a claimable legacy triple key (U21-D4).
 */
export function clearExactStructuredDraftOnSave(
  storage: Storage,
  userId: string | null | undefined,
  templateId: string | null | undefined,
  devVersionId: string | null | undefined,
  anchorId?: string | null,
): void {
  if (!userId || !templateId || !devVersionId) {
    return
  }
  clearStructuredDraft(
    storage,
    buildStructuredDraftStorageKey(userId, templateId, devVersionId, anchorId),
  )
  clearClaimableLegacyStructuredDraft(storage, userId, templateId, devVersionId, anchorId)
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
