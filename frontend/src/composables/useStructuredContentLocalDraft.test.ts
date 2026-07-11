import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import {
  buildStructuredDraftStorageKey,
  clearExactStructuredDraftOnSave,
  clearStructuredDraft,
  readStructuredDraft,
  shouldOfferDraftRecovery,
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

describe('structuredContentDraftStorage', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('BDD-LRP-C2-004 builds isolated keys by user / template / devVersion', () => {
    const a = buildStructuredDraftStorageKey('user-a', 'tpl-1', 'dev-1')
    const b = buildStructuredDraftStorageKey('user-b', 'tpl-1', 'dev-1')
    const c = buildStructuredDraftStorageKey('user-a', 'tpl-1', 'dev-2')
    const d = buildStructuredDraftStorageKey('user-a', 'tpl-2', 'dev-1')

    expect(a).toBe('docgen.structuredDraft.v1:user-a:tpl-1:dev-1')
    expect(a).not.toBe(b)
    expect(a).not.toBe(c)
    expect(a).not.toBe(d)
  })

  it('round-trips a draft payload without undo history fields', () => {
    const key = buildStructuredDraftStorageKey('u1', 't1', 'v1')
    const payload = makePayload()
    expect(writeStructuredDraft(localStorage, key, payload)).toBe(true)

    const read = readStructuredDraft(localStorage, key)
    expect(read).toEqual(payload)
    expect(read).not.toHaveProperty('undoStack')
    expect(JSON.stringify(read)).not.toContain('undo')
  })

  it('clears a draft key', () => {
    const key = buildStructuredDraftStorageKey('u1', 't1', 'v1')
    writeStructuredDraft(localStorage, key, makePayload())
    clearStructuredDraft(localStorage, key)
    expect(readStructuredDraft(localStorage, key)).toBeNull()
  })

  it('BDD-LRP-C2-004 clearExactStructuredDraftOnSave deletes only the exact key (no cross-user/cross-version wipe)', () => {
    const keyA = buildStructuredDraftStorageKey('user-a', 'tpl-1', 'dev-1')
    const keyB = buildStructuredDraftStorageKey('user-b', 'tpl-1', 'dev-1')
    const keyV2 = buildStructuredDraftStorageKey('user-a', 'tpl-1', 'dev-2')
    writeStructuredDraft(localStorage, keyA, makePayload({ structureJson: '{"owner":"a"}' }))
    writeStructuredDraft(localStorage, keyB, makePayload({ structureJson: '{"owner":"b"}' }))
    writeStructuredDraft(localStorage, keyV2, makePayload({ structureJson: '{"owner":"a-v2"}' }))

    // Missing userId must NOT fall back to a templateId sweep (Critical #2 regression).
    clearExactStructuredDraftOnSave(localStorage, null, 'tpl-1', 'dev-1')
    expect(readStructuredDraft(localStorage, keyA)?.structureJson).toBe('{"owner":"a"}')
    expect(readStructuredDraft(localStorage, keyB)?.structureJson).toBe('{"owner":"b"}')
    expect(readStructuredDraft(localStorage, keyV2)?.structureJson).toBe('{"owner":"a-v2"}')

    clearExactStructuredDraftOnSave(localStorage, 'user-a', 'tpl-1', 'dev-1')
    expect(readStructuredDraft(localStorage, keyA)).toBeNull()
    expect(readStructuredDraft(localStorage, keyB)?.structureJson).toBe('{"owner":"b"}')
    expect(readStructuredDraft(localStorage, keyV2)?.structureJson).toBe('{"owner":"a-v2"}')
  })

  it('BDD-LRP-C2-005 save failure retains draft; success clears only the exact key', async () => {
    const key = buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1')
    const otherUserKey = buildStructuredDraftStorageKey('author-2', 'tpl-1', 'dev-1')
    writeStructuredDraft(localStorage, key, makePayload({ structureJson: '{"draft":true}' }))
    writeStructuredDraft(localStorage, otherUserKey, makePayload({ structureJson: '{"other":true}' }))

    const clearOnSuccess = () => {
      clearExactStructuredDraftOnSave(localStorage, 'author-1', 'tpl-1', 'dev-1')
    }

    // Panel contract: clear only on success path — never from `finally` on throw.
    try {
      await Promise.reject(new Error('upsertBinding failed'))
      clearOnSuccess()
    } catch {
      // retain draft
    }
    expect(readStructuredDraft(localStorage, key)?.structureJson).toBe('{"draft":true}')
    expect(readStructuredDraft(localStorage, otherUserKey)?.structureJson).toBe('{"other":true}')

    try {
      await Promise.resolve()
      clearOnSuccess()
    } catch {
      // unused
    }
    expect(readStructuredDraft(localStorage, key)).toBeNull()
    expect(readStructuredDraft(localStorage, otherUserKey)?.structureJson).toBe('{"other":true}')
  })

  it('BDD-LRP-C2-007 does not offer recovery when structure matches server', () => {
    const structure = '{"schemaVersion":"1.0","nodes":[]}'
    expect(shouldOfferDraftRecovery(makePayload({ structureJson: structure }), structure)).toBe(
      false,
    )
    expect(shouldOfferDraftRecovery(null, structure)).toBe(false)
  })

  it('offers recovery when structure differs and anchor matches', () => {
    const draft = makePayload({
      structureJson: '{"schemaVersion":"1.0","nodes":[{"type":"paragraph"}]}',
      anchorId: 'anchor-a',
    })
    expect(
      shouldOfferDraftRecovery(draft, '{"schemaVersion":"1.0","nodes":[]}', 'anchor-a'),
    ).toBe(true)
    expect(
      shouldOfferDraftRecovery(draft, '{"schemaVersion":"1.0","nodes":[]}', 'anchor-b'),
    ).toBe(false)
  })

  it('BDD-LRP-C2-008 evicts oldest other drafts on quota failure then retries', () => {
    const olderKey = buildStructuredDraftStorageKey('u1', 'old-tpl', 'v1')
    const newerKey = buildStructuredDraftStorageKey('u1', 'new-tpl', 'v1')
    const targetKey = buildStructuredDraftStorageKey('u1', 'target-tpl', 'v1')

    writeStructuredDraft(
      localStorage,
      olderKey,
      makePayload({ draftUpdatedAt: '2026-01-01T00:00:00.000Z', structureJson: '{"n":1}' }),
    )
    writeStructuredDraft(
      localStorage,
      newerKey,
      makePayload({ draftUpdatedAt: '2026-06-01T00:00:00.000Z', structureJson: '{"n":2}' }),
    )

    const originalSetItem = Storage.prototype.setItem
    let callCount = 0
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(function (
      this: Storage,
      key: string,
      value: string,
    ) {
      callCount += 1
      if (key === targetKey && callCount === 1) {
        const error = new DOMException('Quota exceeded', 'QuotaExceededError')
        throw error
      }
      return originalSetItem.call(this, key, value)
    })

    const ok = writeStructuredDraft(
      localStorage,
      targetKey,
      makePayload({ draftUpdatedAt: '2026-07-11T12:00:00.000Z', structureJson: '{"n":3}' }),
    )

    expect(ok).toBe(true)
    expect(readStructuredDraft(localStorage, olderKey)).toBeNull()
    expect(readStructuredDraft(localStorage, targetKey)?.structureJson).toBe('{"n":3}')
  })

  it('removes corrupt draft keys on read', () => {
    const key = buildStructuredDraftStorageKey('u1', 't1', 'v1')
    localStorage.setItem(key, '{not-json')
    expect(readStructuredDraft(localStorage, key)).toBeNull()
    expect(localStorage.getItem(key)).toBeNull()
  })
})

describe('useStructuredContentLocalDraft', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    localStorage.clear()
  })

  it('BDD-LRP-C2-001 debounces writes and restores exact structureJson', async () => {
    const userId = ref('author-1')
    const templateId = ref('tpl-1')
    const devVersionId = ref('dev-1')
    const readonly = ref(false)

    const draft = useStructuredContentLocalDraft({
      userId,
      templateId,
      devVersionId,
      readonly,
      debounceMs: 400,
      storage: localStorage,
    })

    const structure = '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"draft"}]}]}'
    draft.scheduleWrite(structure, { serverUpdatedAt: '2026-07-11T01:00:00.000Z', anchorId: 'a1' })
    expect(draft.readDraft()).toBeNull()

    await vi.advanceTimersByTimeAsync(400)
    const stored = draft.readDraft()
    expect(stored?.structureJson).toBe(structure)
    expect(stored?.schemaVersion).toBe(1)
    expect(stored?.anchorId).toBe('a1')

    const server = '{"schemaVersion":"1.0","nodes":[]}'
    expect(draft.evaluateRecovery(server, 'a1')).toEqual(stored)
  })

  it('BDD-LRP-C2-005 clearDraft removes storage (clear-on-save)', async () => {
    const draft = useStructuredContentLocalDraft({
      userId: ref('author-1'),
      templateId: ref('tpl-1'),
      devVersionId: ref('dev-1'),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })

    draft.scheduleWrite('{"schemaVersion":"1.0","nodes":[{"type":"list"}]}')
    await vi.advanceTimersByTimeAsync(0)
    expect(draft.readDraft()).not.toBeNull()

    draft.clearDraft()
    expect(draft.readDraft()).toBeNull()
  })

  it('BDD-LRP-C2-009 skips writes when readonly or scope incomplete', async () => {
    const draft = useStructuredContentLocalDraft({
      userId: ref('author-1'),
      templateId: ref('tpl-1'),
      devVersionId: ref('dev-1'),
      readonly: ref(true),
      debounceMs: 0,
      storage: localStorage,
    })

    draft.scheduleWrite('{"schemaVersion":"1.0","nodes":[{"type":"paragraph"}]}')
    await vi.advanceTimersByTimeAsync(0)
    expect(draft.readDraft()).toBeNull()

    const incomplete = useStructuredContentLocalDraft({
      userId: ref('author-1'),
      templateId: ref('tpl-1'),
      devVersionId: ref(''),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })
    incomplete.scheduleWrite('{"schemaVersion":"1.0","nodes":[{"type":"paragraph"}]}')
    await vi.advanceTimersByTimeAsync(0)
    expect(incomplete.readDraft()).toBeNull()
  })

  it('BDD-LRP-C2-006 clearDraft is explicit — navigation discard must not auto-clear', async () => {
    const draft = useStructuredContentLocalDraft({
      userId: ref('author-1'),
      templateId: ref('tpl-1'),
      devVersionId: ref('dev-1'),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })

    draft.scheduleWrite('{"schemaVersion":"1.0","nodes":[{"type":"paragraph"}]}')
    await vi.advanceTimersByTimeAsync(0)
    // Simulating dirty-guard Discard: composable is disposed without clearDraft
    draft.dispose()
    await nextTick()

    const remounted = useStructuredContentLocalDraft({
      userId: ref('author-1'),
      templateId: ref('tpl-1'),
      devVersionId: ref('dev-1'),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })
    expect(remounted.readDraft()?.structureJson).toContain('paragraph')
  })

  it('BDD-LRP-C2-002 clear-on-save suppress survives subsequent debounce / echo scheduleWrite', async () => {
    const draft = useStructuredContentLocalDraft({
      userId: ref('author-1'),
      templateId: ref('tpl-1'),
      devVersionId: ref('dev-1'),
      readonly: ref(false),
      debounceMs: 400,
      storage: localStorage,
    })

    const savedStructure =
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"saved"}]}]}'
    draft.scheduleWrite(savedStructure)
    await vi.advanceTimersByTimeAsync(400)
    expect(draft.readDraft()?.structureJson).toBe(savedStructure)

    draft.clearDraft({ suppressSubsequentWrites: true })
    expect(draft.readDraft()).toBeNull()
    expect(draft.areWritesSuppressed()).toBe(true)

    // Prop-echo / deep-watch after markPristine must not revive the draft.
    draft.scheduleWrite(savedStructure)
    draft.scheduleWrite(
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"echo-canonicalized"}]}]}',
    )
    await vi.advanceTimersByTimeAsync(400)
    expect(draft.readDraft()).toBeNull()

    // Fresh remount (new composable) with empty storage stays empty and does not
    // re-persist an identical server snapshot as a draft on schedule of baseline.
    const remounted = useStructuredContentLocalDraft({
      userId: ref('author-1'),
      templateId: ref('tpl-1'),
      devVersionId: ref('dev-1'),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })
    expect(remounted.readDraft()).toBeNull()
    expect(remounted.areWritesSuppressed()).toBe(false)
  })

  it('BDD-LRP-C2-005 banner Discard clearDraft does not suppress later edits', async () => {
    const draft = useStructuredContentLocalDraft({
      userId: ref('author-1'),
      templateId: ref('tpl-1'),
      devVersionId: ref('dev-1'),
      readonly: ref(false),
      debounceMs: 0,
      storage: localStorage,
    })

    draft.scheduleWrite('{"schemaVersion":"1.0","nodes":[{"type":"paragraph"}]}')
    await vi.advanceTimersByTimeAsync(0)
    draft.clearDraft()
    expect(draft.areWritesSuppressed()).toBe(false)

    draft.scheduleWrite('{"schemaVersion":"1.0","nodes":[{"type":"list"}]}')
    await vi.advanceTimersByTimeAsync(0)
    expect(draft.readDraft()?.structureJson).toContain('list')
  })
})
