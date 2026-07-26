import { describe, expect, it } from 'vitest'
import {
  friendlyArtifactLabel,
  isArtifactStorageKey,
  resolveFidelityEditAnchorId,
} from '@/utils/fidelityArtifactLabel'

describe('fidelityArtifactLabel (FOS-W4-6)', () => {
  it('detects storage keys', () => {
    expect(isArtifactStorageKey('artifacts/prev-1.docx')).toBe(true)
    expect(isArtifactStorageKey('BODY')).toBe(false)
    expect(isArtifactStorageKey('amountAnchor')).toBe(false)
  })

  it('never resolves edit anchor from storage key', () => {
    expect(
      resolveFidelityEditAnchorId({
        location: null,
        artifact: 'artifacts/prev-1.docx',
      }),
    ).toBeNull()
    expect(
      resolveFidelityEditAnchorId({
        location: 'amountAnchor',
        artifact: 'artifacts/prev-1.docx',
      }),
    ).toBe('amountAnchor')
  })

  it('shows friendly basename for storage-key hints', () => {
    expect(friendlyArtifactLabel(null, 'artifacts/prev-1.docx', 'n/a')).toBe('prev-1.docx')
    expect(friendlyArtifactLabel('BODY', 'artifacts/prev-1.docx', 'n/a')).toBe('BODY')
  })
})
