import type { TemplateExportBundle } from '@/types/template'

export const TEMPLATE_EXPORT_BUNDLE_FORMAT = 'template-export-bundle-v1-json'
export const TEMPLATE_EXPORT_BUNDLE_FORMAT_V2 = 'template-export-bundle-v2-json'

const SUPPORTED_BUNDLE_FORMATS = new Set([
  TEMPLATE_EXPORT_BUNDLE_FORMAT,
  TEMPLATE_EXPORT_BUNDLE_FORMAT_V2,
])

export class TemplateExportBundleParseError extends Error {
  constructor(readonly messageKey: string) {
    super(messageKey)
    this.name = 'TemplateExportBundleParseError'
  }
}

function isSupportedBundleFormat(format: unknown): format is TemplateExportBundle['format'] {
  return typeof format === 'string' && SUPPORTED_BUNDLE_FORMATS.has(format)
}

function normalizeBundle(payload: unknown): TemplateExportBundle {
  if (!payload || typeof payload !== 'object') {
    throw new TemplateExportBundleParseError('templates.import.error.invalidBundle')
  }

  const record = payload as Record<string, unknown>
  const bundleCandidate =
    isSupportedBundleFormat(record.format) && record.metadata
      ? record
      : record.bundle ?? record

  if (!bundleCandidate || typeof bundleCandidate !== 'object') {
    throw new TemplateExportBundleParseError('templates.import.error.invalidBundle')
  }

  const bundle = bundleCandidate as TemplateExportBundle
  if (!isSupportedBundleFormat(bundle.format) || !bundle.metadata) {
    throw new TemplateExportBundleParseError('templates.import.error.unsupportedFormat')
  }

  return bundle
}

async function inflateDeflateRaw(compressed: Uint8Array): Promise<Uint8Array> {
  if (typeof DecompressionStream === 'undefined') {
    throw new TemplateExportBundleParseError('templates.import.error.zipUnsupported')
  }
  const ds = new DecompressionStream('deflate-raw')
  const writer = ds.writable.getWriter()
  await writer.write(compressed)
  await writer.close()
  const decompressed = await new Response(ds.readable).arrayBuffer()
  return new Uint8Array(decompressed)
}

async function readFileText(file: File): Promise<string> {
  if (typeof file.text === 'function') {
    return file.text()
  }
  if (typeof file.arrayBuffer === 'function') {
    return new TextDecoder().decode(await file.arrayBuffer())
  }
  return new Response(file).text()
}

async function readFileArrayBuffer(file: File): Promise<ArrayBuffer> {
  if (typeof file.arrayBuffer === 'function') {
    return file.arrayBuffer()
  }
  return new Response(file).arrayBuffer()
}

async function readSingleEntryZipJson(file: File): Promise<string> {
  const buffer = await readFileArrayBuffer(file)
  const view = new DataView(buffer)
  if (view.byteLength < 30 || view.getUint32(0, true) !== 0x04034b50) {
    throw new TemplateExportBundleParseError('templates.import.error.invalidZip')
  }

  const compressionMethod = view.getUint16(8, true)
  const compressedSize = view.getUint32(18, true)
  const filenameLength = view.getUint16(26, true)
  const extraLength = view.getUint16(28, true)
  const dataOffset = 30 + filenameLength + extraLength

  if (dataOffset + compressedSize > view.byteLength) {
    throw new TemplateExportBundleParseError('templates.import.error.invalidZip')
  }

  const compressed = new Uint8Array(buffer, dataOffset, compressedSize)
  let payload: Uint8Array
  if (compressionMethod === 0) {
    payload = compressed
  } else if (compressionMethod === 8) {
    payload = await inflateDeflateRaw(compressed)
  } else {
    throw new TemplateExportBundleParseError('templates.import.error.zipUnsupported')
  }

  return new TextDecoder().decode(payload)
}

export async function parseTemplateExportBundleFile(file: File): Promise<TemplateExportBundle> {
  const lowerName = file.name.toLowerCase()
  if (lowerName.endsWith('.zip')) {
    const jsonText = await readSingleEntryZipJson(file)
    return normalizeBundle(JSON.parse(jsonText))
  }

  if (lowerName.endsWith('.json')) {
    const text = await readFileText(file)
    return normalizeBundle(JSON.parse(text))
  }

  throw new TemplateExportBundleParseError('templates.import.error.unsupportedFileType')
}

export function buildTemplateExportJsonFilename(externalId: string): string {
  return `${externalId}-export.json`
}

export function buildTemplateExportZipFilename(externalId: string): string {
  return `${externalId}-export.zip`
}
