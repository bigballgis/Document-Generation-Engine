/**
 * LR-C3: bounded undo/redo history for the structured content editor. Keeps a ring buffer
 * of the last N serialized states and exposes `undo()` / `redo()` plus a `can*` flag pair.
 *
 * - States are compared by serialized string to avoid deep-equal cost on large trees.
 * - The buffer is bounded (default 50) to cap memory; oldest states are dropped.
 * - `push(state)` records a new state after an edit; `reset(state)` clears history and
 *   seeds it with the given state (used on initial load / after server save).
 */
import { computed, ref, shallowRef } from 'vue'

export interface UseEditorHistoryOptions {
  /** Maximum number of states retained for undo. Default 50. */
  limit?: number
}

export function useEditorHistory<T>(initial: T, options: UseEditorHistoryOptions = {}) {
  const limit = options.limit ?? 50
  const serialize = (state: T): string =>
    typeof state === 'string' ? state : JSON.stringify(state)

  const states = shallowRef<string[]>([serialize(initial)])
  const cursor = ref(0)
  const present = ref<T>(initial)

  const canUndo = computed(() => cursor.value > 0)
  const canRedo = computed(() => cursor.value < states.value.length - 1)

  function push(state: T) {
    const serialized = serialize(state)
    // Drop the trailing redo tail when a new edit lands after an undo.
    const kept = states.value.slice(0, cursor.value + 1)
    kept.push(serialized)
    // Bound the buffer.
    const overflow = kept.length - limit
    const bounded = overflow > 0 ? kept.slice(overflow) : kept
    states.value = bounded
    cursor.value = bounded.length - 1
    present.value = state
  }

  function reset(state: T) {
    states.value = [serialize(state)]
    cursor.value = 0
    present.value = state
  }

  function undo(): T | null {
    if (!canUndo.value) {
      return null
    }
    cursor.value -= 1
    const serialized = states.value[cursor.value]!
    const restored = (typeof present.value === 'string'
      ? (serialized as unknown as T)
      : (JSON.parse(serialized) as T))
    present.value = restored
    return restored
  }

  function redo(): T | null {
    if (!canRedo.value) {
      return null
    }
    cursor.value += 1
    const serialized = states.value[cursor.value]!
    const restored = (typeof present.value === 'string'
      ? (serialized as unknown as T)
      : (JSON.parse(serialized) as T))
    present.value = restored
    return restored
  }

  return {
    present,
    canUndo,
    canRedo,
    push,
    reset,
    undo,
    redo,
  }
}
