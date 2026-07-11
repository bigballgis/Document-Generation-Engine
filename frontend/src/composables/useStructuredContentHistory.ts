import { computed, ref } from 'vue'

/** Max undo steps retained in memory (C3-C2). */
export const STRUCTURED_CONTENT_HISTORY_CAP = 50

export interface UseStructuredContentHistoryOptions {
  /** Override default cap (tests / future reuse). */
  cap?: number
}

/**
 * In-memory structure-snapshot undo/redo (LR-C3).
 *
 * - Snapshots are serialized structured-content JSON strings.
 * - Cap 50; oldest past entry evicted on overflow.
 * - Optional coalesceKey merges consecutive field edits into one step.
 * - Never persisted — callers must not write stacks into C2 draft blobs.
 */
export function useStructuredContentHistory(options: UseStructuredContentHistoryOptions = {}) {
  const cap = options.cap ?? STRUCTURED_CONTENT_HISTORY_CAP
  const past = ref<string[]>([])
  const future = ref<string[]>([])
  let coalesceKey: string | null = null
  let applyingHistory = false

  const canUndo = computed(() => past.value.length > 0)
  const canRedo = computed(() => future.value.length > 0)
  const undoDepth = computed(() => past.value.length)
  const redoDepth = computed(() => future.value.length)

  function clear(): void {
    past.value = []
    future.value = []
    coalesceKey = null
  }

  function beginApplying(): void {
    applyingHistory = true
  }

  function endApplying(): void {
    applyingHistory = false
  }

  function isApplying(): boolean {
    return applyingHistory
  }

  /**
   * Record a committed structure change.
   * @param beforeSnapshot serialized structure before the mutation
   * @param afterSnapshot serialized structure after the mutation
   * @param nextCoalesceKey optional field-edit key; same key coalesces into one step
   */
  function commit(
    beforeSnapshot: string,
    afterSnapshot: string,
    nextCoalesceKey?: string | null,
  ): void {
    if (applyingHistory) {
      return
    }
    if (beforeSnapshot === afterSnapshot) {
      return
    }

    const key = nextCoalesceKey ?? null
    if (key != null && key === coalesceKey && past.value.length > 0) {
      // Continuing the same field edit — keep the original "before" snapshot.
      future.value = []
      return
    }

    const nextPast = [...past.value, beforeSnapshot]
    if (nextPast.length > cap) {
      past.value = nextPast.slice(nextPast.length - cap)
    } else {
      past.value = nextPast
    }
    future.value = []
    coalesceKey = key
  }

  /** End an active field-edit coalesce session (e.g. on blur). */
  function endCoalesce(): void {
    coalesceKey = null
  }

  function undo(currentSnapshot: string): string | null {
    if (past.value.length === 0) {
      return null
    }
    const previous = past.value[past.value.length - 1]!
    past.value = past.value.slice(0, -1)
    future.value = [...future.value, currentSnapshot]
    coalesceKey = null
    return previous
  }

  function redo(currentSnapshot: string): string | null {
    if (future.value.length === 0) {
      return null
    }
    const next = future.value[future.value.length - 1]!
    future.value = future.value.slice(0, -1)
    past.value = [...past.value, currentSnapshot]
    coalesceKey = null
    return next
  }

  return {
    canUndo,
    canRedo,
    undoDepth,
    redoDepth,
    commit,
    undo,
    redo,
    clear,
    endCoalesce,
    beginApplying,
    endApplying,
    isApplying,
  }
}
