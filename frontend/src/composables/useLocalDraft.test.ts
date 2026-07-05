import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useLocalDraft } from '@/composables/useLocalDraft'

describe('useLocalDraft', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-05T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
    window.localStorage.clear()
  })

  it('writes and recovers a draft', () => {
    const draft = useLocalDraft<string>({ namespace: 'tpl', entityId: 'tpl-1' })
    draft.write('structured-content-json')

    const recovered = draft.tryRecover()
    expect(recovered).not.toBeNull()
    expect(recovered!.state).toBe('structured-content-json')
    expect(recovered!.savedAt).toBe(Date.now())
  })

  it('returns null when no draft exists', () => {
    const draft = useLocalDraft<string>({ namespace: 'tpl', entityId: 'tpl-2' })
    expect(draft.tryRecover()).toBeNull()
  })

  it('clears the draft', () => {
    const draft = useLocalDraft<string>({ namespace: 'tpl', entityId: 'tpl-3' })
    draft.write('state')
    draft.clear()
    expect(draft.tryRecover()).toBeNull()
    expect(window.localStorage.getItem('docgen.draft.tpl.tpl-3')).toBeNull()
  })

  it('ignores drafts older than maxAgeMs', () => {
    const draft = useLocalDraft<string>({ namespace: 'tpl', entityId: 'tpl-4', maxAgeMs: 60_000 })
    draft.write('state')
    // Advance 2 minutes — beyond the 1-minute max age.
    vi.advanceTimersByTime(120_000)
    expect(draft.tryRecover()).toBeNull()
    // The stale entry is removed on read.
    expect(window.localStorage.getItem('docgen.draft.tpl.tpl-4')).toBeNull()
  })

  it('keeps drafts within maxAgeMs', () => {
    const draft = useLocalDraft<string>({ namespace: 'tpl', entityId: 'tpl-5', maxAgeMs: 60_000 })
    draft.write('state')
    vi.advanceTimersByTime(30_000)
    expect(draft.tryRecover()?.state).toBe('state')
  })

  it('isolates drafts by entity id', () => {
    const a = useLocalDraft<string>({ namespace: 'tpl', entityId: 'a' })
    const b = useLocalDraft<string>({ namespace: 'tpl', entityId: 'b' })
    a.write('state-a')
    b.write('state-b')
    expect(a.tryRecover()?.state).toBe('state-a')
    expect(b.tryRecover()?.state).toBe('state-b')
  })
})
