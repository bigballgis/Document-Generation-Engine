import { getCurrentScope, onScopeDispose, unref, type MaybeRef } from 'vue'
import {
  buildStructuredDraftStorageKey,
  clearClaimableLegacyStructuredDraft,
  clearStructuredDraft,
  readStructuredDraft,
  readStructuredDraftForAnchor,
  shouldOfferDraftRecovery,
  writeStructuredDraft,
  type StructuredContentDraftPayload,
} from '@/utils/structuredContentDraftStorage'

const DEFAULT_STRUCTURED_DRAFT_DEBOUNCE_MS = 400

export interface UseStructuredContentLocalDraftOptions {
  userId: MaybeRef<string | null | undefined>
  templateId: MaybeRef<string | null | undefined>
  devVersionId: MaybeRef<string | null | undefined>
  /** When set, draft key includes this anchor (CE-U21). Omit for CM / non-binding mounts. */
  anchorId?: MaybeRef<string | null | undefined>
  readonly: MaybeRef<boolean>
  debounceMs?: number
  storage?: Storage
  now?: () => string
}

export interface ScheduleWriteMeta {
  serverUpdatedAt?: string | null
  anchorId?: string | null
}

function resolveStorage(preferred?: Storage): Storage | null {
  if (preferred) {
    return preferred
  }
  try {
    if (typeof localStorage === 'undefined') {
      return null
    }
    return localStorage
  } catch {
    return null
  }
}

export function useStructuredContentLocalDraft(options: UseStructuredContentLocalDraftOptions) {
  const storage = resolveStorage(options.storage)
  const debounceMs = options.debounceMs ?? DEFAULT_STRUCTURED_DRAFT_DEBOUNCE_MS
  const now = options.now ?? (() => new Date().toISOString())

  let timer: ReturnType<typeof setTimeout> | null = null
  let pendingJson: string | null = null
  let pendingMeta: ScheduleWriteMeta = {}
  /** Bumped on clearDraft so in-flight debounce callbacks cannot rewrite after clear. */
  let writeEpoch = 0
  /**
   * After clear-on-save, block new scheduleWrite calls so prop-echo / remount
   * watchers cannot race the draft back into localStorage (BDD-LRP-C2-002).
   * Banner Discard does not set this — authors may keep editing.
   */
  let writesSuppressed = false

  function resolveScope(): {
    userId: string
    templateId: string
    devVersionId: string
    anchorId: string | null
  } | null {
    const userId = unref(options.userId)
    const templateId = unref(options.templateId)
    const devVersionId = unref(options.devVersionId)
    if (!userId || !templateId || !devVersionId) {
      return null
    }
    const anchorId = unref(options.anchorId) ?? null
    return { userId, templateId, devVersionId, anchorId }
  }

  function resolveKey(): string | null {
    const scope = resolveScope()
    if (!scope) {
      return null
    }
    return buildStructuredDraftStorageKey(
      scope.userId,
      scope.templateId,
      scope.devVersionId,
      scope.anchorId,
    )
  }

  function isWritable(): boolean {
    return !unref(options.readonly) && resolveKey() != null && storage != null
  }

  function migrateLegacyAfterWrite(scope: {
    userId: string
    templateId: string
    devVersionId: string
    anchorId: string | null
  }): void {
    if (!storage || !scope.anchorId) {
      return
    }
    clearClaimableLegacyStructuredDraft(
      storage,
      scope.userId,
      scope.templateId,
      scope.devVersionId,
      scope.anchorId,
    )
  }

  function flushPending(expectedEpoch: number): void {
    timer = null
    if (expectedEpoch !== writeEpoch || writesSuppressed) {
      pendingJson = null
      return
    }
    if (!isWritable() || pendingJson == null || !storage) {
      pendingJson = null
      return
    }
    const scope = resolveScope()
    const key = resolveKey()
    if (!scope || !key) {
      pendingJson = null
      return
    }
    const payload: StructuredContentDraftPayload = {
      schemaVersion: 1,
      structureJson: pendingJson,
      draftUpdatedAt: now(),
      serverUpdatedAt: pendingMeta.serverUpdatedAt ?? null,
      anchorId: pendingMeta.anchorId ?? scope.anchorId,
    }
    writeStructuredDraft(storage, key, payload)
    migrateLegacyAfterWrite(scope)
    pendingJson = null
  }

  function scheduleWrite(structureJson: string, meta: ScheduleWriteMeta = {}): void {
    if (!isWritable() || writesSuppressed) {
      return
    }
    pendingJson = structureJson
    pendingMeta = meta
    if (timer != null) {
      clearTimeout(timer)
    }
    const epoch = writeEpoch
    if (debounceMs <= 0) {
      flushPending(epoch)
      return
    }
    timer = setTimeout(() => {
      flushPending(epoch)
    }, debounceMs)
  }

  function readDraft(): StructuredContentDraftPayload | null {
    if (!storage) {
      return null
    }
    const scope = resolveScope()
    if (!scope) {
      return null
    }
    if (scope.anchorId) {
      return readStructuredDraftForAnchor(
        storage,
        scope.userId,
        scope.templateId,
        scope.devVersionId,
        scope.anchorId,
      )
    }
    return readStructuredDraft(
      storage,
      buildStructuredDraftStorageKey(scope.userId, scope.templateId, scope.devVersionId),
    )
  }

  function clearDraft(clearOptions?: { suppressSubsequentWrites?: boolean }): void {
    writeEpoch += 1
    if (clearOptions?.suppressSubsequentWrites) {
      writesSuppressed = true
    }
    if (timer != null) {
      clearTimeout(timer)
      timer = null
    }
    pendingJson = null
    if (!storage) {
      return
    }
    const scope = resolveScope()
    const key = resolveKey()
    if (!scope || !key) {
      return
    }
    clearStructuredDraft(storage, key)
    migrateLegacyAfterWrite(scope)
  }

  function allowWrites(): void {
    writesSuppressed = false
  }

  function areWritesSuppressed(): boolean {
    return writesSuppressed
  }

  function evaluateRecovery(
    serverStructureJson: string,
    currentAnchorId?: string | null,
  ): StructuredContentDraftPayload | null {
    if (unref(options.readonly)) {
      return null
    }
    const draft = readDraft()
    if (!shouldOfferDraftRecovery(draft, serverStructureJson, currentAnchorId)) {
      return null
    }
    return draft
  }

  function dispose(): void {
    writeEpoch += 1
    if (timer != null) {
      clearTimeout(timer)
      timer = null
    }
    // Intentionally does NOT clear localStorage (BDD-LRP-C2-006).
    pendingJson = null
  }

  if (getCurrentScope()) {
    onScopeDispose(() => {
      dispose()
    })
  }

  return {
    scheduleWrite,
    readDraft,
    clearDraft,
    allowWrites,
    areWritesSuppressed,
    evaluateRecovery,
    dispose,
    resolveKey,
  }
}
