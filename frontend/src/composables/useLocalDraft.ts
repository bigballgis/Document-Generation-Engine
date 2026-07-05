/**
 * LR-C2: local draft recovery for the structured content editor. Persists the editor's
 * serialized state to localStorage keyed by the owning entity (template/content-module id),
 * so that a browser crash, accidental navigation, or session expiry does not lose in-flight
 * authoring work.
 *
 * Contract:
 * - Drafts are LOCAL ONLY — the server save remains the authoritative persistence.
 * - Drafts are cleared on successful server save (the caller invokes `clear()`).
 * - Drafts are keyed by entity id + editor scope so multiple editors do not collide.
 * - Drafts include a timestamp so the UI can show "recovered from X minutes ago".
 */
import { ref, type Ref } from 'vue'

export interface DraftEntry<T> {
  state: T
  savedAt: number
}

export interface UseLocalDraftOptions {
  /** Storage key namespace; combined with the entity id. */
  namespace: string
  /** Entity id (template id, content module id, etc.). */
  entityId: string
  /** Optional max age in ms; older drafts are ignored on load. Default 24h. */
  maxAgeMs?: number
}

const STORAGE_PREFIX = 'docgen.draft.'

export function useLocalDraft<T>(options: UseLocalDraftOptions) {
  const maxAgeMs = options.maxAgeMs ?? 24 * 60 * 60 * 1000
  const storageKey = `${STORAGE_PREFIX}${options.namespace}.${options.entityId}`
  const recovered = ref<DraftEntry<T> | null>(null)

  function read(): DraftEntry<T> | null {
    try {
      const raw = window.localStorage.getItem(storageKey)
      if (!raw) {
        return null
      }
      const entry = JSON.parse(raw) as DraftEntry<T>
      if (typeof entry.savedAt !== 'number') {
        return null
      }
      if (Date.now() - entry.savedAt > maxAgeMs) {
        window.localStorage.removeItem(storageKey)
        return null
      }
      return entry
    } catch {
      return null
    }
  }

  function write(state: T) {
    try {
      const entry: DraftEntry<T> = { state, savedAt: Date.now() }
      window.localStorage.setItem(storageKey, JSON.stringify(entry))
    } catch {
      // Quota exceeded or serialization failure — silently drop; drafts are best-effort.
    }
  }

  function clear() {
    try {
      window.localStorage.removeItem(storageKey)
    } catch {
      // best-effort
    }
    recovered.value = null
  }

  function tryRecover(): DraftEntry<T> | null {
    const entry = read()
    recovered.value = entry
    return entry
  }

  return {
    recovered: recovered as Ref<DraftEntry<T> | null>,
    write,
    clear,
    tryRecover,
  }
}
