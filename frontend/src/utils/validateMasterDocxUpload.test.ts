import { describe, expect, it } from 'vitest'
import {
  MASTER_DOCX_MAX_UPLOAD_BYTES,
  validateMasterDocxUploadFile,
} from '@/utils/validateMasterDocxUpload'

function makeFile(name: string, size: number, type = ''): File {
  const blob = new Blob([new Uint8Array(Math.min(size, 64))], { type })
  const file = new File([blob], name, { type })
  Object.defineProperty(file, 'size', { value: size })
  return file
}

describe('validateMasterDocxUploadFile', () => {
  it('accepts a .docx at or under the 50MB limit', () => {
    expect(validateMasterDocxUploadFile(makeFile('letterhead.docx', MASTER_DOCX_MAX_UPLOAD_BYTES))).toEqual({
      ok: true,
    })
    expect(
      validateMasterDocxUploadFile(
        makeFile(
          'letterhead.docx',
          1024,
          'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        ),
      ),
    ).toEqual({ ok: true })
  })

  it('rejects files larger than 50MB', () => {
    expect(
      validateMasterDocxUploadFile(makeFile('huge.docx', MASTER_DOCX_MAX_UPLOAD_BYTES + 1)),
    ).toEqual({ ok: false, messageKey: 'masters.upload.errorTooLarge' })
  })

  it('rejects non-.docx filenames', () => {
    expect(validateMasterDocxUploadFile(makeFile('notes.pdf', 1024))).toEqual({
      ok: false,
      messageKey: 'masters.upload.errorDocxOnly',
    })
  })

  it('rejects non-whitelisted Content-Type when present', () => {
    expect(validateMasterDocxUploadFile(makeFile('evil.docx', 1024, 'text/html'))).toEqual({
      ok: false,
      messageKey: 'masters.upload.errorDocxOnly',
    })
  })

  it('allows missing Content-Type when suffix is .docx', () => {
    expect(validateMasterDocxUploadFile(makeFile('letterhead.docx', 1024, ''))).toEqual({ ok: true })
  })
})
