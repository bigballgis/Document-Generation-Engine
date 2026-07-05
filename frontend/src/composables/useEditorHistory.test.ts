import { describe, expect, it } from 'vitest'
import { useEditorHistory } from '@/composables/useEditorHistory'

describe('useEditorHistory', () => {
  it('starts with no undo/redo available', () => {
    const h = useEditorHistory('a')
    expect(h.canUndo.value).toBe(false)
    expect(h.canRedo.value).toBe(false)
  })

  it('pushes states and undoes back to the previous state', () => {
    const h = useEditorHistory('a')
    h.push('b')
    h.push('c')
    expect(h.present.value).toBe('c')
    expect(h.canUndo.value).toBe(true)
    expect(h.undo()).toBe('b')
    expect(h.undo()).toBe('a')
    expect(h.canUndo.value).toBe(false)
  })

  it('redoes after undo', () => {
    const h = useEditorHistory('a')
    h.push('b')
    h.push('c')
    h.undo()
    h.undo()
    expect(h.present.value).toBe('a')
    expect(h.redo()).toBe('b')
    expect(h.redo()).toBe('c')
    expect(h.canRedo.value).toBe(false)
  })

  it('drops the redo tail when a new edit lands after undo', () => {
    const h = useEditorHistory('a')
    h.push('b')
    h.push('c')
    h.undo()
    expect(h.present.value).toBe('b')
    h.push('d')
    expect(h.present.value).toBe('d')
    expect(h.canRedo.value).toBe(false)
    expect(h.undo()).toBe('b')
    expect(h.undo()).toBe('a')
  })

  it('bounds the history to the configured limit', () => {
    const h = useEditorHistory(0, { limit: 3 })
    h.push(1)
    h.push(2)
    h.push(3)
    h.push(4)
    // After 4 pushes + initial, the buffer is bounded to 3; oldest (0,1) dropped.
    expect(h.canUndo.value).toBe(true)
    // Undo walks back within the bounded window; at most limit-1 undos.
    let count = 0
    while (h.canUndo.value) {
      h.undo()
      count++
    }
    expect(count).toBeLessThanOrEqual(2)
  })

  it('reset clears history and seeds with the given state', () => {
    const h = useEditorHistory('a')
    h.push('b')
    h.push('c')
    h.reset('z')
    expect(h.present.value).toBe('z')
    expect(h.canUndo.value).toBe(false)
    expect(h.canRedo.value).toBe(false)
  })

  it('works with object states via JSON serialization', () => {
    const h = useEditorHistory<{ v: number }>({ v: 1 })
    h.push({ v: 2 })
    h.push({ v: 3 })
    const restored = h.undo()
    expect(restored).toEqual({ v: 2 })
    expect(h.present.value).toEqual({ v: 2 })
  })
})
