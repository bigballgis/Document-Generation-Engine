import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import {
  buildStructuredDraftStorageKey,
  clearExactStructuredDraftOnSave,
  readStructuredDraft,
  writeStructuredDraft,
  type StructuredContentDraftPayload,
} from '@/utils/structuredContentDraftStorage'
import { useStructuredContentLocalDraft } from '@/composables/useStructuredContentLocalDraft'

function makePayload(
  overrides: Partial<StructuredContentDraftPayload> = {},
): StructuredContentDraftPayload {
  return {
    schemaVersion: 1,
    structureJson: '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[]}]}',
    draftUpdatedAt: '2026-07-11T02:00:00.000Z',
    serverUpdatedAt: '2026-07-11T01:00:00.000Z',
    anchorId: null,
    ...overrides,
  }
}

describe('CE-U21 structured draft per-anchor keys', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    localStorage.clear()
  })

  it('BDD-CE-U21-DAC-001 builds four-tuple key when anchorId is present', () => {
    expect(buildStructuredDraftStorageKey('U', 'T', 'V', 'A')).toBe(
      'docgen.structuredDraft.v1:U:T:V:A',
    )
    expect(buildStructuredDraftStorageKey('U', 'T', 'V')).toBe('docgen.structuredDraft.v1:U:T:V')
  })

  it('BDD-CE-U21-DAC-002 isolates drafts across anchors under the same template+devVersion', async () => {
    const draftA = useStructuredContentLocalDraft({
      userId: ref('U'),
      templateId: ref('T'),
      devVersionId: ref('V'),
      anchorId: ref('A'),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })
    const draftB = useStructuredContentLocalDraft({
      userId: ref('U'),
      templateId: ref('T'),
      devVersionId: ref('V'),
      anchorId: ref('B'),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })

    draftA.scheduleWrite('{"owner":"SA"}', { anchorId: 'A' })
    await vi.advanceTimersByTimeAsync(0)
    draftB.scheduleWrite('{"owner":"SB"}', { anchorId: 'B' })
    await vi.advanceTimersByTimeAsync(0)

    expect(draftA.readDraft()?.structureJson).toBe('{"owner":"SA"}')
    expect(draftB.readDraft()?.structureJson).toBe('{"owner":"SB"}')
    expect(localStorage.getItem(buildStructuredDraftStorageKey('U', 'T', 'V', 'A'))).not.toBeNull()
    expect(localStorage.getItem(buildStructuredDraftStorageKey('U', 'T', 'V', 'B'))).not.toBeNull()
    expect(localStorage.getItem(buildStructuredDraftStorageKey('U', 'T', 'V'))).toBeNull()
  })

  it('BDD-CE-U21-DAC-003 offers recovery from legacy triple key and migrates on write', async () => {
    const legacyKey = buildStructuredDraftStorageKey('U', 'T', 'V')
    writeStructuredDraft(
      localStorage,
      legacyKey,
      makePayload({ structureJson: '{"legacy":"A"}', anchorId: 'A' }),
    )

    const draft = useStructuredContentLocalDraft({
      userId: ref('U'),
      templateId: ref('T'),
      devVersionId: ref('V'),
      anchorId: ref('A'),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })

    expect(draft.evaluateRecovery('{"server":true}', 'A')?.structureJson).toBe('{"legacy":"A"}')

    draft.scheduleWrite('{"migrated":true}', { anchorId: 'A' })
    await vi.advanceTimersByTimeAsync(0)

    expect(localStorage.getItem(legacyKey)).toBeNull()
    expect(
      readStructuredDraft(localStorage, buildStructuredDraftStorageKey('U', 'T', 'V', 'A'))
        ?.structureJson,
    ).toBe('{"migrated":true}')
  })

  it('BDD-CE-U21-DAC-004 does not claim legacy key belonging to another anchor', async () => {
    const legacyKey = buildStructuredDraftStorageKey('U', 'T', 'V')
    writeStructuredDraft(
      localStorage,
      legacyKey,
      makePayload({ structureJson: '{"legacy":"B"}', anchorId: 'B' }),
    )

    const draft = useStructuredContentLocalDraft({
      userId: ref('U'),
      templateId: ref('T'),
      devVersionId: ref('V'),
      anchorId: ref('A'),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })

    expect(draft.evaluateRecovery('{"server":true}', 'A')).toBeNull()
    draft.scheduleWrite('{"A":true}', { anchorId: 'A' })
    await vi.advanceTimersByTimeAsync(0)

    expect(readStructuredDraft(localStorage, legacyKey)?.structureJson).toBe('{"legacy":"B"}')
    expect(
      readStructuredDraft(localStorage, buildStructuredDraftStorageKey('U', 'T', 'V', 'A'))
        ?.structureJson,
    ).toBe('{"A":true}')
  })

  it('BDD-CE-U21-DAC-005 clearExactStructuredDraftOnSave clears only the current anchor key', () => {
    const keyA = buildStructuredDraftStorageKey('U', 'T', 'V', 'A')
    const keyB = buildStructuredDraftStorageKey('U', 'T', 'V', 'B')
    writeStructuredDraft(localStorage, keyA, makePayload({ structureJson: '{"A":true}' }))
    writeStructuredDraft(localStorage, keyB, makePayload({ structureJson: '{"B":true}' }))

    clearExactStructuredDraftOnSave(localStorage, 'U', 'T', 'V', 'A')

    expect(readStructuredDraft(localStorage, keyA)).toBeNull()
    expect(readStructuredDraft(localStorage, keyB)?.structureJson).toBe('{"B":true}')
  })
})
