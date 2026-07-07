import { inflateRawSync } from 'node:zlib'

import AdmZip from 'adm-zip'

export interface AssertDocxArtifactOptions {
  minBytes: number
  forbiddenPatterns?: string[]
  contentMarkers?: string[]
  requireDocumentXml?: boolean
}

const DEFAULT_FORBIDDEN_PATTERNS = ['LOREM', '{{', '}}', 'PLACEHOLDER', 'TODO'] as const

function readUInt16LE(buffer: Buffer, offset: number): number {
  return buffer.readUInt16LE(offset)
}

function readUInt32LE(buffer: Buffer, offset: number): number {
  return buffer.readUInt32LE(offset)
}

function extractZipEntry(buffer: Buffer, entryName: string): Buffer | null {
  const target = entryName.replace(/\\/g, '/')
  let offset = 0

  while (offset + 30 <= buffer.length) {
    const signature = readUInt32LE(buffer, offset)
    if (signature !== 0x04034b50) {
      break
    }

    const compressionMethod = readUInt16LE(buffer, offset + 8)
    const compressedSize = readUInt32LE(buffer, offset + 18)
    const fileNameLength = readUInt16LE(buffer, offset + 26)
    const extraFieldLength = readUInt16LE(buffer, offset + 28)
    const nameStart = offset + 30
    const nameEnd = nameStart + fileNameLength
    const fileName = buffer.toString('utf8', nameStart, nameEnd)
    const dataStart = nameEnd + extraFieldLength
    const dataEnd = dataStart + compressedSize

    if (dataEnd > buffer.length) {
      break
    }

    if (fileName === target) {
      const compressed = buffer.subarray(dataStart, dataEnd)
      if (compressionMethod === 0) {
        return Buffer.from(compressed)
      }
      if (compressionMethod === 8) {
        return Buffer.from(inflateRawSync(compressed))
      }
      throw new Error(`Unsupported ZIP compression method ${compressionMethod} for ${entryName}`)
    }

    offset = dataEnd
  }

  return null
}

function hasZipEntry(buffer: Buffer, entryName: string): boolean {
  if (extractZipEntry(buffer, entryName) !== null) {
    return true
  }
  try {
    const zip = new AdmZip(buffer)
    return zip.getEntry(entryName) !== null
  } catch {
    return false
  }
}

function extractDocumentXmlText(docx: Buffer): string {
  const documentXml = extractZipEntry(docx, 'word/document.xml')
  if (documentXml) {
    return documentXml.toString('utf8')
  }

  try {
    const zip = new AdmZip(docx)
    const entry = zip.getEntry('word/document.xml')
    if (entry) {
      return entry.getData().toString('utf8')
    }
  } catch {
    // Fall through to latin1 fallback below.
  }

  return docx.toString('latin1')
}

export function assertDocxArtifact(body: Buffer, options: AssertDocxArtifactOptions): void {
  if (body.length < 4 || body[0] !== 0x50 || body[1] !== 0x4b || body[2] !== 0x03 || body[3] !== 0x04) {
    throw new Error('Response body is not a valid DOCX (missing PK\\x03\\x04 magic bytes)')
  }

  if (body.length < options.minBytes) {
    throw new Error(`DOCX too small: ${body.length} bytes (minimum ${options.minBytes})`)
  }

  if (options.requireDocumentXml !== false && !hasZipEntry(body, 'word/document.xml')) {
    throw new Error('DOCX archive is missing required entry word/document.xml')
  }

  const documentText = extractDocumentXmlText(body)
  const forbiddenPatterns = options.forbiddenPatterns ?? [...DEFAULT_FORBIDDEN_PATTERNS]

  for (const pattern of forbiddenPatterns) {
    if (documentText.includes(pattern)) {
      throw new Error(`DOCX contains forbidden placeholder marker: ${pattern}`)
    }
  }

  for (const marker of options.contentMarkers ?? []) {
    if (!documentText.includes(marker)) {
      throw new Error(`DOCX is missing expected content marker: ${marker}`)
    }
  }
}

/** Test helper — builds a minimal uncompressed DOCX archive with document.xml text. */
export function buildMinimalDocxArchive(innerText: string, padBytes = 0): Buffer {
  const fileName = 'word/document.xml'
  const xml =
    `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>` +
    `<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">` +
    `<w:body><w:p><w:r><w:t>${innerText}</w:t></w:r></w:p></w:body></w:document>`
  const content = Buffer.from(xml, 'utf8')
  const nameBytes = Buffer.from(fileName, 'utf8')

  const localHeader = Buffer.alloc(30 + nameBytes.length)
  localHeader.writeUInt32LE(0x04034b50, 0)
  localHeader.writeUInt16LE(20, 4)
  localHeader.writeUInt16LE(0, 6)
  localHeader.writeUInt16LE(0, 8)
  localHeader.writeUInt16LE(0, 10)
  localHeader.writeUInt16LE(0, 12)
  localHeader.writeUInt32LE(content.length, 14)
  localHeader.writeUInt32LE(content.length, 18)
  localHeader.writeUInt16LE(nameBytes.length, 26)
  localHeader.writeUInt16LE(0, 28)
  nameBytes.copy(localHeader, 30)

  const centralDirectory = Buffer.alloc(46 + nameBytes.length)
  centralDirectory.writeUInt32LE(0x02014b50, 0)
  centralDirectory.writeUInt16LE(20, 4)
  centralDirectory.writeUInt16LE(20, 6)
  centralDirectory.writeUInt16LE(0, 8)
  centralDirectory.writeUInt16LE(0, 10)
  centralDirectory.writeUInt16LE(0, 12)
  centralDirectory.writeUInt16LE(0, 14)
  centralDirectory.writeUInt32LE(content.length, 16)
  centralDirectory.writeUInt32LE(content.length, 20)
  centralDirectory.writeUInt16LE(nameBytes.length, 28)
  centralDirectory.writeUInt16LE(0, 30)
  centralDirectory.writeUInt16LE(0, 32)
  centralDirectory.writeUInt16LE(0, 34)
  centralDirectory.writeUInt16LE(0, 36)
  centralDirectory.writeUInt32LE(0, 38)
  centralDirectory.writeUInt32LE(0, 42)
  nameBytes.copy(centralDirectory, 46)

  const endOfCentralDirectory = Buffer.alloc(22)
  endOfCentralDirectory.writeUInt32LE(0x06054b50, 0)
  endOfCentralDirectory.writeUInt16LE(0, 4)
  endOfCentralDirectory.writeUInt16LE(0, 6)
  endOfCentralDirectory.writeUInt16LE(1, 8)
  endOfCentralDirectory.writeUInt16LE(1, 10)
  endOfCentralDirectory.writeUInt32LE(centralDirectory.length, 12)
  endOfCentralDirectory.writeUInt32LE(localHeader.length + content.length, 16)
  endOfCentralDirectory.writeUInt16LE(0, 20)

  const padding = padBytes > 0 ? Buffer.alloc(padBytes, 0x20) : Buffer.alloc(0)
  return Buffer.concat([localHeader, content, centralDirectory, endOfCentralDirectory, padding])
}
