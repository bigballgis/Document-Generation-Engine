/** Mirrors `DemoPublishRegistry.allPublishExternalIds()` / `Get-DemoPublishExternalIds` (keep-set). */
export const DEMO_PUBLISH_EXTERNAL_IDS = [
  'CORP-FOL-OFFER',
  'DEMO-CREDIT-LIMIT-CONFIRM',
  'DEMO-ANNUAL-REVIEW',
  'DEMO-FACILITY-RENEWAL',
  'DEMO-FACILITY-AMENDMENT',
  'DEMO-COMMITMENT-LETTER',
  'DEMO-FORMAL-DEMAND',
  'DEMO-COVENANT-WAIVER',
] as const

export type DemoPublishExternalId = (typeof DEMO_PUBLISH_EXTERNAL_IDS)[number]

/**
 * Per-template DOCX size floors (BDD-DEMO-TYP-012 anti-scaffold guard).
 * Keep-set floors aligned to `deploy/demo-shared/demo-runtime-generate-manifest.json`.
 */
export const DEMO_RUNTIME_MIN_DOCX_BYTES: Record<DemoPublishExternalId, number> = {
  'CORP-FOL-OFFER': 20_480,
  'DEMO-CREDIT-LIMIT-CONFIRM': 7_680,
  'DEMO-ANNUAL-REVIEW': 5_120,
  'DEMO-FACILITY-RENEWAL': 4_608,
  'DEMO-FACILITY-AMENDMENT': 6_144,
  'DEMO-COMMITMENT-LETTER': 6_144,
  'DEMO-FORMAL-DEMAND': 5_120,
  'DEMO-COVENANT-WAIVER': 5_120,
}
