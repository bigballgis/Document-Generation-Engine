import { describe, expect, it } from 'vitest'
import {
  STRUCTURED_CONTENT_HISTORY_CAP,
  useStructuredContentHistory,
} from '@/composables/useStructuredContentHistory'

const S0 = '{"schemaVersion":"1.0","nodes":[]}'
const S1 = '{"schemaVersion":"1.0","nodes":[{"type":"paragraph"}]}'
const S2 = '{"schemaVersion":"1.0","nodes":[{"type":"sectionHeading"}]}'
const S3 = '{"schemaVersion":"1.0","nodes":[{"type":"list"}]}'
const S4 = '{"schemaVersion":"1.0","nodes":[{"type":"conditionBlock"}]}'

describe('useStructuredContentHistory', () => {
  it('BDD-LRP-C3-001 push then undo×2 restores first-step state', () => {
    const history = useStructuredContentHistory()

    history.commit(S0, S1)
    history.commit(S1, S2)
    history.commit(S2, S3)

    expect(history.canUndo.value).toBe(true)
    expect(history.canRedo.value).toBe(false)

    const afterFirstUndo = history.undo(S3)
    expect(afterFirstUndo).toBe(S2)

    const afterSecondUndo = history.undo(S2)
    expect(afterSecondUndo).toBe(S1)

    expect(history.canRedo.value).toBe(true)
    expect(history.canUndo.value).toBe(true)
  })

  it('BDD-LRP-C3-002 caps undo depth at 50 and drops oldest', () => {
    const history = useStructuredContentHistory()
    const snapshots: string[] = [S0]

    for (let i = 0; i < STRUCTURED_CONTENT_HISTORY_CAP + 1; i += 1) {
      const before = snapshots[i]!
      const after = `{"schemaVersion":"1.0","nodes":[{"type":"paragraph","i":${i}}]}`
      snapshots.push(after)
      history.commit(before, after)
    }

    expect(history.undoDepth.value).toBe(STRUCTURED_CONTENT_HISTORY_CAP)

    // Oldest (S0 → first edit) was evicted; cannot restore S0 via undo chain.
    let current = snapshots[snapshots.length - 1]!
    for (let i = 0; i < STRUCTURED_CONTENT_HISTORY_CAP; i += 1) {
      const restored = history.undo(current)
      expect(restored).not.toBeNull()
      current = restored!
    }
    expect(history.canUndo.value).toBe(false)
    expect(current).not.toBe(S0)
  })

  it('BDD-LRP-C3-003 redo restores pre-undo snapshot', () => {
    const history = useStructuredContentHistory()
    history.commit(S0, S1)
    history.commit(S1, S2)

    expect(history.undo(S2)).toBe(S1)
    expect(history.redo(S1)).toBe(S2)
  })

  it('BDD-LRP-C3-004 new commit after undo truncates redo branch', () => {
    const history = useStructuredContentHistory()
    history.commit(S0, S1)
    history.commit(S1, S2)
    history.undo(S2)

    expect(history.canRedo.value).toBe(true)
    history.commit(S1, S4)

    expect(history.canRedo.value).toBe(false)
    expect(history.redo(S4)).toBeNull()
  })

  it('BDD-LRP-C3-005 empty stacks disable undo/redo', () => {
    const history = useStructuredContentHistory()
    expect(history.canUndo.value).toBe(false)
    expect(history.canRedo.value).toBe(false)
    expect(history.undo(S0)).toBeNull()
    expect(history.redo(S0)).toBeNull()
  })

  it('BDD-LRP-C3-015 identical before/after does not push', () => {
    const history = useStructuredContentHistory()
    history.commit(S1, S1)
    expect(history.canUndo.value).toBe(false)
  })

  it('BDD-LRP-C3-014 coalesces same-field edits into one step', () => {
    const history = useStructuredContentHistory()
    const key = 'field:0:value'
    const t0 = '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":""}]}]}'
    const t1 = '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"a"}]}]}'
    const t2 = '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"ab"}]}]}'
    const t3 = '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"abc"}]}]}'

    history.commit(t0, t1, key)
    history.commit(t1, t2, key)
    history.commit(t2, t3, key)

    expect(history.undoDepth.value).toBe(1)
    expect(history.undo(t3)).toBe(t0)
  })

  it('ends coalesce when a structural commit uses a different key', () => {
    const history = useStructuredContentHistory()
    const key = 'field:0:value'
    history.commit(S0, S1, key)
    history.commit(S1, S2, key)
    history.commit(S2, S3) // structural — no coalesce key

    expect(history.undoDepth.value).toBe(2)
    expect(history.undo(S3)).toBe(S2)
    expect(history.undo(S2)).toBe(S0)
  })

  it('BDD-LRP-C3-009 clear empties undo and redo', () => {
    const history = useStructuredContentHistory()
    history.commit(S0, S1)
    history.commit(S1, S2)
    history.undo(S2)
    history.clear()

    expect(history.canUndo.value).toBe(false)
    expect(history.canRedo.value).toBe(false)
    expect(history.undoDepth.value).toBe(0)
  })

  it('skips recording while applyingHistory is active', () => {
    const history = useStructuredContentHistory()
    history.commit(S0, S1)
    history.beginApplying()
    history.commit(S1, S2)
    history.endApplying()

    expect(history.undoDepth.value).toBe(1)
    expect(history.undo(S1)).toBe(S0)
  })
})
