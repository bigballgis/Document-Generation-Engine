import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, type Locator, type Page } from '@playwright/test'

const E2E_DIR = path.dirname(fileURLToPath(import.meta.url))

export const P14_T01_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P14-T01')
export const P14_T01_SCREENSHOT_DIR = path.join(P14_T01_EVIDENCE_ROOT, 'screenshots')

export const P14_T02_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P14-T02')
export const P14_T02_SCREENSHOT_DIR = path.join(P14_T02_EVIDENCE_ROOT, 'screenshots')

export const P14_T03_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P14-T03')
export const P14_T03_SCREENSHOT_DIR = path.join(P14_T03_EVIDENCE_ROOT, 'screenshots')

export const P18_T10_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P18-T10')
export const P18_T10_SCREENSHOT_DIR = path.join(P18_T10_EVIDENCE_ROOT, 'screenshots')

export const P21_T01_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P21-T01')
export const P21_T01_SCREENSHOT_DIR = path.join(P21_T01_EVIDENCE_ROOT, 'screenshots')

export const P21_T01A_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P21-T01a')
export const P21_T01A_SCREENSHOT_DIR = path.join(P21_T01A_EVIDENCE_ROOT, 'screenshots')

export const P14_T01_VIEWPORT = { width: 1440, height: 900 } as const
export const P14_T02_VIEWPORT = { width: 1440, height: 900 } as const
export const P14_T03_VIEWPORT = { width: 1440, height: 900 } as const
export const P18_T10_VIEWPORT = { width: 1440, height: 900 } as const
export const P21_T01_VIEWPORT = { width: 1440, height: 900 } as const
export const P21_T01A_VIEWPORT = { width: 1440, height: 900 } as const

export type BrandPreset = 'REDBC' | 'GREENBC'

export function ensureP14EvidenceDirs(): void {
  fs.mkdirSync(P14_T01_SCREENSHOT_DIR, { recursive: true })
}

export function ensureP14T02EvidenceDirs(): void {
  fs.mkdirSync(P14_T02_SCREENSHOT_DIR, { recursive: true })
}

export function ensureP14T03EvidenceDirs(): void {
  fs.mkdirSync(P14_T03_SCREENSHOT_DIR, { recursive: true })
}

export function ensureP18T10EvidenceDirs(): void {
  fs.mkdirSync(P18_T10_SCREENSHOT_DIR, { recursive: true })
}

export function ensureP21T01EvidenceDirs(): void {
  fs.mkdirSync(P21_T01_SCREENSHOT_DIR, { recursive: true })
}

export function ensureP21T01aEvidenceDirs(): void {
  fs.mkdirSync(P21_T01A_SCREENSHOT_DIR, { recursive: true })
}

export function p14ScreenshotPath(filename: string): string {
  return path.join(P14_T01_SCREENSHOT_DIR, filename)
}

export function p14T02ScreenshotPath(filename: string): string {
  return path.join(P14_T02_SCREENSHOT_DIR, filename)
}

export function p14T03ScreenshotPath(filename: string): string {
  return path.join(P14_T03_SCREENSHOT_DIR, filename)
}

export function p18T10ScreenshotPath(filename: string): string {
  return path.join(P18_T10_SCREENSHOT_DIR, filename)
}

export function p21T01ScreenshotPath(filename: string): string {
  return path.join(P21_T01_SCREENSHOT_DIR, filename)
}

export function p21T01aScreenshotPath(filename: string): string {
  return path.join(P21_T01A_SCREENSHOT_DIR, filename)
}

export async function captureP14Screenshot(page: Page, filename: string): Promise<string> {
  ensureP14EvidenceDirs()
  const target = p14ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP14LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP14EvidenceDirs()
  const target = p14ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** Brand switcher option labels are localized (en + zh-CN). */
const BRAND_OPTION_NAME: Record<BrandPreset, RegExp> = {
  REDBC: /^(red bank|红色银行)$/i,
  GREENBC: /^(green bank|绿色银行)$/i,
}

/**
 * LR-C8 onboarding tour mask (`el-tour__hollow`) intercepts header clicks
 * (including `.brand-switcher`) until Skip/Finish. First-run tour can mount
 * after login navigation — short 3s polls miss it. Wait longer, force-click
 * Skip, Esc if only the hollow is up, and retry until both are gone.
 */
export async function dismissOnboardingTourIfPresent(
  page: Page,
  options?: { appearTimeoutMs?: number },
): Promise<void> {
  const appearTimeoutMs = options?.appearTimeoutMs ?? 12_000
  const skipTour = page.getByTestId('onboarding-tour-skip')
  const hollow = page.locator('.el-tour__hollow')

  const skipAppeared = await skipTour
    .waitFor({ state: 'visible', timeout: appearTimeoutMs })
    .then(() => true)
    .catch(() => false)
  const hollowAppeared =
    skipAppeared ||
    (await hollow
      .first()
      .waitFor({ state: 'visible', timeout: Math.min(2_000, appearTimeoutMs) })
      .then(() => true)
      .catch(() => false))

  if (!skipAppeared && !hollowAppeared) {
    if ((await skipTour.count()) === 0 && (await hollow.count()) === 0) {
      return
    }
  }

  for (let attempt = 0; attempt < 3; attempt += 1) {
    if ((await skipTour.count()) === 0 && (await hollow.count()) === 0) {
      return
    }
    if ((await skipTour.count()) > 0) {
      await skipTour.click({ force: true })
    } else {
      // close-on-press-escape is enabled on OnboardingTour el-tour
      await page.keyboard.press('Escape')
    }
    try {
      await expect
        .poll(async () => (await skipTour.count()) + (await hollow.count()), {
          timeout: 5_000,
          intervals: [100, 200, 400],
        })
        .toBe(0)
    } catch {
      // Retry loop continues if tour/hollow still settling.
    }
  }

  await expect(skipTour).toHaveCount(0)
  await expect(hollow).toHaveCount(0)
}

export async function switchBrand(page: Page, brand: BrandPreset): Promise<void> {
  // Tour may appear after the post-login dismiss window; clear mask before header click.
  await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 2_000 })
  const brandSwitcher = page.locator('.brand-switcher')
  try {
    await brandSwitcher.click({ timeout: 5_000 })
  } catch {
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 8_000 })
    await brandSwitcher.click()
  }
  await page.getByRole('option', { name: BRAND_OPTION_NAME[brand] }).click()
  await expect(page.locator('html')).toHaveAttribute('data-brand', brand)
}

export async function captureBrandHeader(page: Page, filename: string): Promise<string> {
  return captureP14LocatorScreenshot(page.locator('.shell-header .header-brand'), filename)
}

export async function captureP14T02Screenshot(page: Page, filename: string): Promise<string> {
  ensureP14T02EvidenceDirs()
  const target = p14T02ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP14T02LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP14T02EvidenceDirs()
  const target = p14T02ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export async function captureP14T03Screenshot(page: Page, filename: string): Promise<string> {
  ensureP14T03EvidenceDirs()
  const target = p14T03ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP14T03LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP14T03EvidenceDirs()
  const target = p14T03ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export async function captureP18T10Screenshot(page: Page, filename: string): Promise<string> {
  ensureP18T10EvidenceDirs()
  const target = p18T10ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP18T10LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP18T10EvidenceDirs()
  const target = p18T10ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export async function captureP21T01Screenshot(page: Page, filename: string): Promise<string> {
  ensureP21T01EvidenceDirs()
  const target = p21T01ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP21T01LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP21T01EvidenceDirs()
  const target = p21T01ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export async function captureP21T01aScreenshot(page: Page, filename: string): Promise<string> {
  ensureP21T01aEvidenceDirs()
  const target = p21T01aScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP21T01aLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP21T01aEvidenceDirs()
  const target = p21T01aScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P21_T01B_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P21-T01b')
export const P21_T01B_SCREENSHOT_DIR = path.join(P21_T01B_EVIDENCE_ROOT, 'screenshots')
export const P21_T01B_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP21T01bEvidenceDirs(): void {
  fs.mkdirSync(P21_T01B_SCREENSHOT_DIR, { recursive: true })
}

export function p21T01bScreenshotPath(filename: string): string {
  return path.join(P21_T01B_SCREENSHOT_DIR, filename)
}

export async function captureP21T01bScreenshot(page: Page, filename: string): Promise<string> {
  ensureP21T01bEvidenceDirs()
  const target = p21T01bScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP21T01bLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP21T01bEvidenceDirs()
  const target = p21T01bScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P21_T03_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P21-T03')
export const P21_T03_SCREENSHOT_DIR = path.join(P21_T03_EVIDENCE_ROOT, 'screenshots')
export const P21_T03_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP21T03EvidenceDirs(): void {
  fs.mkdirSync(P21_T03_SCREENSHOT_DIR, { recursive: true })
}

export async function captureP21T03LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP21T03EvidenceDirs()
  const target = path.join(P21_T03_SCREENSHOT_DIR, filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P21_T04_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P21-T04')
export const P21_T04_SCREENSHOT_DIR = path.join(P21_T04_EVIDENCE_ROOT, 'screenshots')
export const P21_T04_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP21T04EvidenceDirs(): void {
  fs.mkdirSync(P21_T04_SCREENSHOT_DIR, { recursive: true })
}

export async function captureP21T04LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP21T04EvidenceDirs()
  const target = path.join(P21_T04_SCREENSHOT_DIR, filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P21_T05_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P21-T05')
export const P21_T05_SCREENSHOT_DIR = path.join(P21_T05_EVIDENCE_ROOT, 'screenshots')
export const P21_T05_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP21T05EvidenceDirs(): void {
  fs.mkdirSync(P21_T05_SCREENSHOT_DIR, { recursive: true })
}

export async function captureP21T05LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP21T05EvidenceDirs()
  const target = path.join(P21_T05_SCREENSHOT_DIR, filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P21_T08_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P21-T08')
export const P21_T08_SCREENSHOT_DIR = path.join(P21_T08_EVIDENCE_ROOT, 'screenshots')
export const P21_T08_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP21T08EvidenceDirs(): void {
  fs.mkdirSync(P21_T08_SCREENSHOT_DIR, { recursive: true })
}

export async function captureP21T08LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP21T08EvidenceDirs()
  const target = path.join(P21_T08_SCREENSHOT_DIR, filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P21_T09_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P21-T09')
export const P21_T09_SCREENSHOT_DIR = path.join(P21_T09_EVIDENCE_ROOT, 'screenshots')
export const P21_T09_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP21T09EvidenceDirs(): void {
  fs.mkdirSync(P21_T09_SCREENSHOT_DIR, { recursive: true })
}

export async function captureP21T09LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP21T09EvidenceDirs()
  const target = path.join(P21_T09_SCREENSHOT_DIR, filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P12_AUD_B10_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P12-AUD-B10')
export const P12_AUD_B10_SCREENSHOT_DIR = path.join(P12_AUD_B10_EVIDENCE_ROOT, 'screenshots')
export const P12_AUD_B10_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP12AudB10EvidenceDirs(): void {
  fs.mkdirSync(P12_AUD_B10_SCREENSHOT_DIR, { recursive: true })
}

export function p12AudB10ScreenshotPath(filename: string): string {
  return path.join(P12_AUD_B10_SCREENSHOT_DIR, filename)
}

export async function captureP12AudB10Screenshot(page: Page, filename: string): Promise<string> {
  ensureP12AudB10EvidenceDirs()
  const target = p12AudB10ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP12AudB10LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP12AudB10EvidenceDirs()
  const target = p12AudB10ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P12_RISK_PROMPT_UX_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'P12-BDD-RISK-PROMPT-UX-001',
)
export const P12_RISK_PROMPT_UX_SCREENSHOT_DIR = path.join(
  P12_RISK_PROMPT_UX_EVIDENCE_ROOT,
  'screenshots',
)
export const P12_RISK_PROMPT_UX_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP12RiskPromptUxEvidenceDirs(): void {
  fs.mkdirSync(P12_RISK_PROMPT_UX_SCREENSHOT_DIR, { recursive: true })
}

export function p12RiskPromptUxScreenshotPath(filename: string): string {
  return path.join(P12_RISK_PROMPT_UX_SCREENSHOT_DIR, filename)
}

export async function captureP12RiskPromptUxScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureP12RiskPromptUxEvidenceDirs()
  const target = p12RiskPromptUxScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP12RiskPromptUxLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP12RiskPromptUxEvidenceDirs()
  const target = p12RiskPromptUxScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P2_T06_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'P2-T06')
export const P2_T06_SCREENSHOT_DIR = path.join(P2_T06_EVIDENCE_ROOT, 'screenshots')
export const P2_T06_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP2T06EvidenceDirs(): void {
  fs.mkdirSync(P2_T06_SCREENSHOT_DIR, { recursive: true })
}

export function p2T06ScreenshotPath(filename: string): string {
  return path.join(P2_T06_SCREENSHOT_DIR, filename)
}

export async function captureP2T06Screenshot(page: Page, filename: string): Promise<string> {
  ensureP2T06EvidenceDirs()
  const target = p2T06ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export type AppLocale = 'en' | 'zh-CN'

const LOCALE_OPTION_NAME: Record<AppLocale, RegExp> = {
  en: /^english$/i,
  'zh-CN': /^(chinese \(simplified\)|简体中文)$/i,
}

export async function switchLocale(page: Page, locale: AppLocale): Promise<void> {
  const localeSwitcher = page.locator('.locale-switcher')
  await localeSwitcher.click()
  await page.getByRole('option', { name: LOCALE_OPTION_NAME[locale] }).click()
  await expect(page.locator('html')).toHaveAttribute('lang', locale)
}

export const DEMO_FULL_FLOW_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'demo-full-lifecycle')
export const DEMO_FULL_FLOW_SCREENSHOT_DIR = path.join(DEMO_FULL_FLOW_EVIDENCE_ROOT, 'screenshots')
export const DEMO_FULL_FLOW_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureDemoFullFlowEvidenceDirs(): void {
  fs.mkdirSync(DEMO_FULL_FLOW_SCREENSHOT_DIR, { recursive: true })
}

export async function captureDemoFullFlowLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureDemoFullFlowEvidenceDirs()
  const target = path.join(DEMO_FULL_FLOW_SCREENSHOT_DIR, filename)
  await locator.screenshot({ path: target })
  return filename
}

export async function captureDemoFullFlowScreenshot(page: Page, filename: string): Promise<string> {
  ensureDemoFullFlowEvidenceDirs()
  const target = path.join(DEMO_FULL_FLOW_SCREENSHOT_DIR, filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export const P12_TEMPLATE_TESTING_OVERHAUL_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'P12-TEMPLATE-TESTING-OVERHAUL',
)
export const P12_TEMPLATE_TESTING_OVERHAUL_SCREENSHOT_DIR = path.join(
  P12_TEMPLATE_TESTING_OVERHAUL_EVIDENCE_ROOT,
  'screenshots',
)
export const P12_TEMPLATE_TESTING_OVERHAUL_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP12TemplateTestingOverhaulEvidenceDirs(): void {
  fs.mkdirSync(P12_TEMPLATE_TESTING_OVERHAUL_SCREENSHOT_DIR, { recursive: true })
}

export function p12TemplateTestingOverhaulScreenshotPath(filename: string): string {
  return path.join(P12_TEMPLATE_TESTING_OVERHAUL_SCREENSHOT_DIR, filename)
}

export async function captureP12TemplateTestingOverhaulScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureP12TemplateTestingOverhaulEvidenceDirs()
  const target = p12TemplateTestingOverhaulScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP12TemplateTestingOverhaulLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP12TemplateTestingOverhaulEvidenceDirs()
  const target = p12TemplateTestingOverhaulScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const LRP_B6_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'LRP-B6')
export const LRP_B6_SCREENSHOT_DIR = path.join(LRP_B6_EVIDENCE_ROOT, 'screenshots')
export const LRP_B6_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureLrpB6EvidenceDirs(): void {
  fs.mkdirSync(LRP_B6_SCREENSHOT_DIR, { recursive: true })
}

export function lrpB6ScreenshotPath(filename: string): string {
  return path.join(LRP_B6_SCREENSHOT_DIR, filename)
}

export async function captureLrpB6Screenshot(page: Page, filename: string): Promise<string> {
  ensureLrpB6EvidenceDirs()
  const target = lrpB6ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureLrpB6LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureLrpB6EvidenceDirs()
  const target = lrpB6ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P12_API_PACKAGE_ACCESS_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'P12-API-PACKAGE-ACCESS',
)
export const P12_API_PACKAGE_ACCESS_SCREENSHOT_DIR = path.join(
  P12_API_PACKAGE_ACCESS_EVIDENCE_ROOT,
  'screenshots',
)
export const P12_API_PACKAGE_ACCESS_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP12ApiPackageAccessEvidenceDirs(): void {
  fs.mkdirSync(P12_API_PACKAGE_ACCESS_SCREENSHOT_DIR, { recursive: true })
}

export function p12ApiPackageAccessScreenshotPath(filename: string): string {
  return path.join(P12_API_PACKAGE_ACCESS_SCREENSHOT_DIR, filename)
}

export async function captureP12ApiPackageAccessScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureP12ApiPackageAccessEvidenceDirs()
  const target = p12ApiPackageAccessScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureP12ApiPackageAccessLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP12ApiPackageAccessEvidenceDirs()
  const target = p12ApiPackageAccessScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const P13_EXTERNAL_SERVICES_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'P13-EXTERNAL-SERVICES',
)
export const P13_EXTERNAL_SERVICES_SCREENSHOT_DIR = path.join(
  P13_EXTERNAL_SERVICES_EVIDENCE_ROOT,
  'screenshots',
)
export const P13_EXTERNAL_SERVICES_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureP13ExternalServicesEvidenceDirs(): void {
  fs.mkdirSync(P13_EXTERNAL_SERVICES_SCREENSHOT_DIR, { recursive: true })
}

export function p13ExternalServicesScreenshotPath(filename: string): string {
  return path.join(P13_EXTERNAL_SERVICES_SCREENSHOT_DIR, filename)
}

export async function captureP13ExternalServicesLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureP13ExternalServicesEvidenceDirs()
  const target = p13ExternalServicesScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export async function captureP13ExternalServicesScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureP13ExternalServicesEvidenceDirs()
  const target = p13ExternalServicesScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: true })
  return filename
}

export const UX_ENTITY_DISPLAY_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'UX-ENTITY-DISPLAY',
)
export const UX_ENTITY_DISPLAY_SCREENSHOT_DIR = path.join(
  UX_ENTITY_DISPLAY_EVIDENCE_ROOT,
  'screenshots',
)
export const UX_ENTITY_DISPLAY_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureUxEntityDisplayEvidenceDirs(): void {
  fs.mkdirSync(UX_ENTITY_DISPLAY_SCREENSHOT_DIR, { recursive: true })
}

export function uxEntityDisplayScreenshotPath(filename: string): string {
  return path.join(UX_ENTITY_DISPLAY_SCREENSHOT_DIR, filename)
}

export async function captureUxEntityDisplayScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureUxEntityDisplayEvidenceDirs()
  const target = uxEntityDisplayScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureUxEntityDisplayLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureUxEntityDisplayEvidenceDirs()
  const target = uxEntityDisplayScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const CDP_E2E_T01_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CDP-E2E-T01')
export const CDP_E2E_T01_SCREENSHOT_DIR = path.join(CDP_E2E_T01_EVIDENCE_ROOT, 'screenshots')
export const CDP_E2E_T01_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCdpE2eT01EvidenceDirs(): void {
  fs.mkdirSync(CDP_E2E_T01_SCREENSHOT_DIR, { recursive: true })
}

export function cdpE2eT01ScreenshotPath(filename: string): string {
  return path.join(CDP_E2E_T01_SCREENSHOT_DIR, filename)
}

export async function captureCdpE2eT01Screenshot(page: Page, filename: string): Promise<string> {
  ensureCdpE2eT01EvidenceDirs()
  const target = cdpE2eT01ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCdpE2eT01LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCdpE2eT01EvidenceDirs()
  const target = cdpE2eT01ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** Shared 1920×1080 viewport for CDP CD-2 decision/publish UIUX evidence (T02–T05). */
export const CDP_E2E_CD2_DECISION_VIEWPORT = { width: 1920, height: 1080 } as const

export type CdpE2eCd2DecisionTaskId =
  | 'CDP-E2E-T02'
  | 'CDP-E2E-T03'
  | 'CDP-E2E-T04'
  | 'CDP-E2E-T05'
  | 'CDP-E2E-T06'
  | 'CDP-E2E-T07'
  | 'CDP-E2E-T08'
  | 'CDP-E2E-T09'
  | 'CDP-E2E-T10'
  | 'CDP-E2E-T11'
  | 'CDP-E2E-T12'
  | 'CDP-E2E-T13'

function cdpE2eDecisionEvidenceRoot(taskId: CdpE2eCd2DecisionTaskId): string {
  return path.join(E2E_DIR, '..', 'evidence', taskId)
}

function cdpE2eDecisionScreenshotDir(taskId: CdpE2eCd2DecisionTaskId): string {
  return path.join(cdpE2eDecisionEvidenceRoot(taskId), 'screenshots')
}

export function ensureCdpE2eDecisionEvidenceDirs(taskId: CdpE2eCd2DecisionTaskId): void {
  fs.mkdirSync(cdpE2eDecisionScreenshotDir(taskId), { recursive: true })
}

export function cdpE2eDecisionScreenshotPath(
  taskId: CdpE2eCd2DecisionTaskId,
  filename: string,
): string {
  return path.join(cdpE2eDecisionScreenshotDir(taskId), filename)
}

export async function captureCdpE2eDecisionScreenshot(
  page: Page,
  taskId: CdpE2eCd2DecisionTaskId,
  filename: string,
): Promise<string> {
  ensureCdpE2eDecisionEvidenceDirs(taskId)
  const target = cdpE2eDecisionScreenshotPath(taskId, filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCdpE2eDecisionLocatorScreenshot(
  locator: Locator,
  taskId: CdpE2eCd2DecisionTaskId,
  filename: string,
): Promise<string> {
  ensureCdpE2eDecisionEvidenceDirs(taskId)
  const target = cdpE2eDecisionScreenshotPath(taskId, filename)
  await locator.screenshot({ path: target })
  return filename
}

export const LRP_C9_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'LRP-C9')
export const LRP_C9_SCREENSHOT_DIR = path.join(LRP_C9_EVIDENCE_ROOT, 'screenshots')
export const LRP_C9_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureLrpC9EvidenceDirs(): void {
  fs.mkdirSync(LRP_C9_SCREENSHOT_DIR, { recursive: true })
}

export function lrpC9ScreenshotPath(filename: string): string {
  return path.join(LRP_C9_SCREENSHOT_DIR, filename)
}

export async function captureLrpC9Screenshot(page: Page, filename: string): Promise<string> {
  ensureLrpC9EvidenceDirs()
  const target = lrpC9ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureLrpC9LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureLrpC9EvidenceDirs()
  const target = lrpC9ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const F7_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'F7')
export const F7_SCREENSHOT_DIR = path.join(F7_EVIDENCE_ROOT, 'screenshots')
export const F7_VIEWPORT = { width: 1440, height: 900 } as const
export const F7_NARROW_VIEWPORT = { width: 375, height: 812 } as const

export function ensureF7EvidenceDirs(): void {
  fs.mkdirSync(F7_SCREENSHOT_DIR, { recursive: true })
}

export function f7ScreenshotPath(filename: string): string {
  return path.join(F7_SCREENSHOT_DIR, filename)
}

export async function captureF7Screenshot(page: Page, filename: string): Promise<string> {
  ensureF7EvidenceDirs()
  const target = f7ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureF7LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureF7EvidenceDirs()
  const target = f7ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const LRP_C2_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'LRP-C2')
export const LRP_C2_SCREENSHOT_DIR = path.join(LRP_C2_EVIDENCE_ROOT, 'screenshots')
export const LRP_C2_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureLrpC2EvidenceDirs(): void {
  fs.mkdirSync(LRP_C2_SCREENSHOT_DIR, { recursive: true })
}

export function lrpC2ScreenshotPath(filename: string): string {
  return path.join(LRP_C2_SCREENSHOT_DIR, filename)
}

export async function captureLrpC2Screenshot(page: Page, filename: string): Promise<string> {
  ensureLrpC2EvidenceDirs()
  const target = lrpC2ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureLrpC2LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureLrpC2EvidenceDirs()
  const target = lrpC2ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const LRP_C3_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'LRP-C3')
export const LRP_C3_SCREENSHOT_DIR = path.join(LRP_C3_EVIDENCE_ROOT, 'screenshots')
export const LRP_C3_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureLrpC3EvidenceDirs(): void {
  fs.mkdirSync(LRP_C3_SCREENSHOT_DIR, { recursive: true })
}

export function lrpC3ScreenshotPath(filename: string): string {
  return path.join(LRP_C3_SCREENSHOT_DIR, filename)
}

export async function captureLrpC3Screenshot(page: Page, filename: string): Promise<string> {
  ensureLrpC3EvidenceDirs()
  const target = lrpC3ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureLrpC3LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureLrpC3EvidenceDirs()
  const target = lrpC3ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const LRP_C5_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'LRP-C5')
export const LRP_C5_SCREENSHOT_DIR = path.join(LRP_C5_EVIDENCE_ROOT, 'screenshots')
export const LRP_C5_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureLrpC5EvidenceDirs(): void {
  fs.mkdirSync(LRP_C5_SCREENSHOT_DIR, { recursive: true })
}

export function lrpC5ScreenshotPath(filename: string): string {
  return path.join(LRP_C5_SCREENSHOT_DIR, filename)
}

export async function captureLrpC5Screenshot(page: Page, filename: string): Promise<string> {
  ensureLrpC5EvidenceDirs()
  const target = lrpC5ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureLrpC5LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureLrpC5EvidenceDirs()
  const target = lrpC5ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const LRP_C6_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'LRP-C6')
export const LRP_C6_SCREENSHOT_DIR = path.join(LRP_C6_EVIDENCE_ROOT, 'screenshots')
export const LRP_C6_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureLrpC6EvidenceDirs(): void {
  fs.mkdirSync(LRP_C6_SCREENSHOT_DIR, { recursive: true })
}

export function lrpC6ScreenshotPath(filename: string): string {
  return path.join(LRP_C6_SCREENSHOT_DIR, filename)
}

export async function captureLrpC6Screenshot(page: Page, filename: string): Promise<string> {
  ensureLrpC6EvidenceDirs()
  const target = lrpC6ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureLrpC6LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureLrpC6EvidenceDirs()
  const target = lrpC6ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const LRP_C7_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'LRP-C7')
export const LRP_C7_SCREENSHOT_DIR = path.join(LRP_C7_EVIDENCE_ROOT, 'screenshots')
export const LRP_C7_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureLrpC7EvidenceDirs(): void {
  fs.mkdirSync(LRP_C7_SCREENSHOT_DIR, { recursive: true })
}

export function lrpC7ScreenshotPath(filename: string): string {
  return path.join(LRP_C7_SCREENSHOT_DIR, filename)
}

export async function captureLrpC7Screenshot(page: Page, filename: string): Promise<string> {
  ensureLrpC7EvidenceDirs()
  const target = lrpC7ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureLrpC7LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureLrpC7EvidenceDirs()
  const target = lrpC7ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const LRP_C8_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'LRP-C8')
export const LRP_C8_SCREENSHOT_DIR = path.join(LRP_C8_EVIDENCE_ROOT, 'screenshots')
export const LRP_C8_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureLrpC8EvidenceDirs(): void {
  fs.mkdirSync(LRP_C8_SCREENSHOT_DIR, { recursive: true })
}

export function lrpC8ScreenshotPath(filename: string): string {
  return path.join(LRP_C8_SCREENSHOT_DIR, filename)
}

export async function captureLrpC8Screenshot(page: Page, filename: string): Promise<string> {
  ensureLrpC8EvidenceDirs()
  const target = lrpC8ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureLrpC8LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureLrpC8EvidenceDirs()
  const target = lrpC8ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const API_OPS_DISCOVERABILITY_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'API-OPS-DISCOVERABILITY',
)
export const API_OPS_DISCOVERABILITY_SCREENSHOT_DIR = path.join(
  API_OPS_DISCOVERABILITY_EVIDENCE_ROOT,
  'screenshots',
)
export const API_OPS_DISCOVERABILITY_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureApiOpsDiscoverabilityEvidenceDirs(): void {
  fs.mkdirSync(API_OPS_DISCOVERABILITY_SCREENSHOT_DIR, { recursive: true })
}

export function apiOpsDiscoverabilityScreenshotPath(filename: string): string {
  return path.join(API_OPS_DISCOVERABILITY_SCREENSHOT_DIR, filename)
}

export async function captureApiOpsDiscoverabilityScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureApiOpsDiscoverabilityEvidenceDirs()
  const target = apiOpsDiscoverabilityScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureApiOpsDiscoverabilityLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureApiOpsDiscoverabilityEvidenceDirs()
  const target = apiOpsDiscoverabilityScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U03 schema-driven test data dialog — 1920×1080 dual-brand evidence. */
export const CE_U03_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U03-testdata-schema-form')
export const CE_U03_SCREENSHOT_DIR = path.join(CE_U03_EVIDENCE_ROOT, 'screenshots')
export const CE_U03_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU03EvidenceDirs(): void {
  fs.mkdirSync(CE_U03_SCREENSHOT_DIR, { recursive: true })
}

export function ceU03ScreenshotPath(filename: string): string {
  return path.join(CE_U03_SCREENSHOT_DIR, filename)
}

export async function captureCeU03Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU03EvidenceDirs()
  const target = ceU03ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU03LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU03EvidenceDirs()
  const target = ceU03ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U08 content-module review loop — 1920×1080 dual-brand evidence. */
export const CE_U08_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U08')
export const CE_U08_SCREENSHOT_DIR = path.join(CE_U08_EVIDENCE_ROOT, 'screenshots')
export const CE_U08_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU08EvidenceDirs(): void {
  fs.mkdirSync(CE_U08_SCREENSHOT_DIR, { recursive: true })
}

export function ceU08ScreenshotPath(filename: string): string {
  return path.join(CE_U08_SCREENSHOT_DIR, filename)
}

export async function captureCeU08Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU08EvidenceDirs()
  const target = ceU08ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU08LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU08EvidenceDirs()
  const target = ceU08ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U09 master review reachability — 1920×1080 dual-brand evidence. */
export const CE_U09_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U09')
export const CE_U09_SCREENSHOT_DIR = path.join(CE_U09_EVIDENCE_ROOT, 'screenshots')
export const CE_U09_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU09EvidenceDirs(): void {
  fs.mkdirSync(CE_U09_SCREENSHOT_DIR, { recursive: true })
}

export function ceU09ScreenshotPath(filename: string): string {
  return path.join(CE_U09_SCREENSHOT_DIR, filename)
}

export async function captureCeU09Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU09EvidenceDirs()
  const target = ceU09ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU09LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU09EvidenceDirs()
  const target = ceU09ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U10 sharedGroupCodes create/settings/summary — 1920×1080 dual-brand evidence. */
export const CE_U10_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U10')
export const CE_U10_SCREENSHOT_DIR = path.join(CE_U10_EVIDENCE_ROOT, 'screenshots')
export const CE_U10_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU10EvidenceDirs(): void {
  fs.mkdirSync(CE_U10_SCREENSHOT_DIR, { recursive: true })
}

export function ceU10ScreenshotPath(filename: string): string {
  return path.join(CE_U10_SCREENSHOT_DIR, filename)
}

export async function captureCeU10Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU10EvidenceDirs()
  const target = ceU10ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU10LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU10EvidenceDirs()
  const target = ceU10ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U11 invocation troubleshoot — release filter / export / error envelope @1440×900. */
export const CE_U11_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U11')
export const CE_U11_SCREENSHOT_DIR = path.join(CE_U11_EVIDENCE_ROOT, 'screenshots')
export const CE_U11_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureCeU11EvidenceDirs(): void {
  fs.mkdirSync(CE_U11_SCREENSHOT_DIR, { recursive: true })
}

export function ceU11ScreenshotPath(filename: string): string {
  return path.join(CE_U11_SCREENSHOT_DIR, filename)
}

export async function captureCeU11Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU11EvidenceDirs()
  const target = ceU11ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU11LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU11EvidenceDirs()
  const target = ceU11ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U12 caller contract copyable examples — 1920×1080 dual-brand evidence. */
export const CE_U12_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U12')
export const CE_U12_SCREENSHOT_DIR = path.join(CE_U12_EVIDENCE_ROOT, 'screenshots')
export const CE_U12_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU12EvidenceDirs(): void {
  fs.mkdirSync(CE_U12_SCREENSHOT_DIR, { recursive: true })
}

export function ceU12ScreenshotPath(filename: string): string {
  return path.join(CE_U12_SCREENSHOT_DIR, filename)
}

export async function captureCeU12Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU12EvidenceDirs()
  const target = ceU12ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU12LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU12EvidenceDirs()
  const target = ceU12ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U14 dashboard lifecycle todos — 1920×1080 dual-brand evidence. */
export const CE_U14_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U14')
export const CE_U14_SCREENSHOT_DIR = path.join(CE_U14_EVIDENCE_ROOT, 'screenshots')
export const CE_U14_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU14EvidenceDirs(): void {
  fs.mkdirSync(CE_U14_SCREENSHOT_DIR, { recursive: true })
}

export function ceU14ScreenshotPath(filename: string): string {
  return path.join(CE_U14_SCREENSHOT_DIR, filename)
}

export async function captureCeU14Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU14EvidenceDirs()
  const target = ceU14ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU14LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU14EvidenceDirs()
  const target = ceU14ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U15 lifecycle stepper + Go fix — 1920×1080 dual-brand evidence. */
export const CE_U15_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U15')
export const CE_U15_SCREENSHOT_DIR = path.join(CE_U15_EVIDENCE_ROOT, 'screenshots')
export const CE_U15_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU15EvidenceDirs(): void {
  fs.mkdirSync(CE_U15_SCREENSHOT_DIR, { recursive: true })
}

export function ceU15ScreenshotPath(filename: string): string {
  return path.join(CE_U15_SCREENSHOT_DIR, filename)
}

export async function captureCeU15Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU15EvidenceDirs()
  const target = ceU15ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU15LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU15EvidenceDirs()
  const target = ceU15ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U16 authoring path compress — 1920×1080 dual-brand evidence. */
export const CE_U16_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U16')
export const CE_U16_SCREENSHOT_DIR = path.join(CE_U16_EVIDENCE_ROOT, 'screenshots')
export const CE_U16_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU16EvidenceDirs(): void {
  fs.mkdirSync(CE_U16_SCREENSHOT_DIR, { recursive: true })
}

export function ceU16ScreenshotPath(filename: string): string {
  return path.join(CE_U16_SCREENSHOT_DIR, filename)
}

export async function captureCeU16Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU16EvidenceDirs()
  const target = ceU16ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU16LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU16EvidenceDirs()
  const target = ceU16ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U17 editor shortcuts + command-palette Actions — 1920×1080 dual-brand evidence. */
export const CE_U17_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U17')
export const CE_U17_SCREENSHOT_DIR = path.join(CE_U17_EVIDENCE_ROOT, 'screenshots')
export const CE_U17_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU17EvidenceDirs(): void {
  fs.mkdirSync(CE_U17_SCREENSHOT_DIR, { recursive: true })
}

export function ceU17ScreenshotPath(filename: string): string {
  return path.join(CE_U17_SCREENSHOT_DIR, filename)
}

export async function captureCeU17Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU17EvidenceDirs()
  const target = ceU17ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU17LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU17EvidenceDirs()
  const target = ceU17ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-K05 master impact real — impact panel / replace confirm / revision diff @1440×900. */
export const CE_K05_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-K05')
export const CE_K05_SCREENSHOT_DIR = path.join(CE_K05_EVIDENCE_ROOT, 'screenshots')
export const CE_K05_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureCeK05EvidenceDirs(): void {
  fs.mkdirSync(CE_K05_SCREENSHOT_DIR, { recursive: true })
}

export function ceK05ScreenshotPath(filename: string): string {
  return path.join(CE_K05_SCREENSHOT_DIR, filename)
}

export async function captureCeK05Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeK05EvidenceDirs()
  const target = ceK05ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeK05LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeK05EvidenceDirs()
  const target = ceK05ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U06 master anchor position overview — 1440×900 dual-brand evidence. */
export const CE_U06_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U06')
export const CE_U06_SCREENSHOT_DIR = path.join(CE_U06_EVIDENCE_ROOT, 'screenshots')
export const CE_U06_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureCeU06EvidenceDirs(): void {
  fs.mkdirSync(CE_U06_SCREENSHOT_DIR, { recursive: true })
}

export function ceU06ScreenshotPath(filename: string): string {
  return path.join(CE_U06_SCREENSHOT_DIR, filename)
}

export async function captureCeU06Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU06EvidenceDirs()
  const target = ceU06ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU06LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU06EvidenceDirs()
  const target = ceU06ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-G03 test-data PII badges / handling / explicit confirm — 1440×900 dual-brand. */
export const CE_G03_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-G03-testdata-pii')
export const CE_G03_SCREENSHOT_DIR = path.join(CE_G03_EVIDENCE_ROOT, 'screenshots')
export const CE_G03_VIEWPORT = { width: 1440, height: 900 } as const
export const CE_G03_NARROW_VIEWPORT = { width: 1280, height: 800 } as const

export function ensureCeG03EvidenceDirs(): void {
  fs.mkdirSync(CE_G03_SCREENSHOT_DIR, { recursive: true })
}

export function ceG03ScreenshotPath(filename: string): string {
  return path.join(CE_G03_SCREENSHOT_DIR, filename)
}

export async function captureCeG03Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeG03EvidenceDirs()
  const target = ceG03ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeG03LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeG03EvidenceDirs()
  const target = ceG03ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-E02 Asset Library admin — list / upload / disable @1440×900 dual-brand. */
export const CE_E02_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-E02-asset-library')
export const CE_E02_SCREENSHOT_DIR = path.join(CE_E02_EVIDENCE_ROOT, 'screenshots')
export const CE_E02_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureCeE02EvidenceDirs(): void {
  fs.mkdirSync(CE_E02_SCREENSHOT_DIR, { recursive: true })
}

export function ceE02ScreenshotPath(filename: string): string {
  return path.join(CE_E02_SCREENSHOT_DIR, filename)
}

export async function captureCeE02Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeE02EvidenceDirs()
  const target = ceE02ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeE02LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeE02EvidenceDirs()
  const target = ceE02ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-G04 Legal Hold admin — list / create / release @1440×900 dual-brand. */
export const CE_G04_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-G04-legal-hold')
export const CE_G04_SCREENSHOT_DIR = path.join(CE_G04_EVIDENCE_ROOT, 'screenshots')
export const CE_G04_VIEWPORT = { width: 1440, height: 900 } as const
export const CE_G04_NARROW_VIEWPORT = { width: 1280, height: 800 } as const

export function ensureCeG04EvidenceDirs(): void {
  fs.mkdirSync(CE_G04_SCREENSHOT_DIR, { recursive: true })
}

export function ceG04ScreenshotPath(filename: string): string {
  return path.join(CE_G04_SCREENSHOT_DIR, filename)
}

export async function captureCeG04Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeG04EvidenceDirs()
  const target = ceG04ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeG04LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeG04EvidenceDirs()
  const target = ceG04ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U18 batch-test history sampleResults expand — 1920×1080 dual-brand evidence. */
export const CE_U18_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U18')
export const CE_U18_SCREENSHOT_DIR = path.join(CE_U18_EVIDENCE_ROOT, 'screenshots')
export const CE_U18_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU18EvidenceDirs(): void {
  fs.mkdirSync(CE_U18_SCREENSHOT_DIR, { recursive: true })
}

export function ceU18ScreenshotPath(filename: string): string {
  return path.join(CE_U18_SCREENSHOT_DIR, filename)
}

export async function captureCeU18Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU18EvidenceDirs()
  const target = ceU18ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU18LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU18EvidenceDirs()
  const target = ceU18ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U19 Package Hub Dependencies tab (read-only) — 1920×1080 dual-brand evidence. */
export const CE_U19_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U19')
export const CE_U19_SCREENSHOT_DIR = path.join(CE_U19_EVIDENCE_ROOT, 'screenshots')
export const CE_U19_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU19EvidenceDirs(): void {
  fs.mkdirSync(CE_U19_SCREENSHOT_DIR, { recursive: true })
}

export function ceU19ScreenshotPath(filename: string): string {
  return path.join(CE_U19_SCREENSHOT_DIR, filename)
}

export async function captureCeU19Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU19EvidenceDirs()
  const target = ceU19ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU19LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU19EvidenceDirs()
  const target = ceU19ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U20 clause create structured editor + catalog Status — 1920×1080 dual-brand evidence. */
export const CE_U20_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U20')
export const CE_U20_SCREENSHOT_DIR = path.join(CE_U20_EVIDENCE_ROOT, 'screenshots')
export const CE_U20_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU20EvidenceDirs(): void {
  fs.mkdirSync(CE_U20_SCREENSHOT_DIR, { recursive: true })
}

export function ceU20ScreenshotPath(filename: string): string {
  return path.join(CE_U20_SCREENSHOT_DIR, filename)
}

export async function captureCeU20Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU20EvidenceDirs()
  const target = ceU20ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU20LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU20EvidenceDirs()
  const target = ceU20ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-U21 per-anchor draft recovery + binding 409 conflict — 1920×1080 dual-brand evidence. */
export const CE_U21_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-U21')
export const CE_U21_SCREENSHOT_DIR = path.join(CE_U21_EVIDENCE_ROOT, 'screenshots')
export const CE_U21_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureCeU21EvidenceDirs(): void {
  fs.mkdirSync(CE_U21_SCREENSHOT_DIR, { recursive: true })
}

export function ceU21ScreenshotPath(filename: string): string {
  return path.join(CE_U21_SCREENSHOT_DIR, filename)
}

export async function captureCeU21Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeU21EvidenceDirs()
  const target = ceU21ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeU21LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeU21EvidenceDirs()
  const target = ceU21ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** CE-G05 annual review + clause FULL_TEXT / where-used — 1440×900 dual-brand evidence. */
export const CE_G05_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'CE-G05')
export const CE_G05_SCREENSHOT_DIR = path.join(CE_G05_EVIDENCE_ROOT, 'screenshots')
export const CE_G05_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureCeG05EvidenceDirs(): void {
  fs.mkdirSync(CE_G05_SCREENSHOT_DIR, { recursive: true })
}

export function ceG05ScreenshotPath(filename: string): string {
  return path.join(CE_G05_SCREENSHOT_DIR, filename)
}

export async function captureCeG05Screenshot(page: Page, filename: string): Promise<string> {
  ensureCeG05EvidenceDirs()
  const target = ceG05ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureCeG05LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureCeG05EvidenceDirs()
  const target = ceG05ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** PRR-D01c / #136 Dashboard Overview summary API — 1920×1080 dual-brand evidence. */
export const PRR_D01C_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'PRR-D01C')
export const PRR_D01C_SCREENSHOT_DIR = path.join(PRR_D01C_EVIDENCE_ROOT, 'screenshots')
export const PRR_D01C_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensurePrrD01cEvidenceDirs(): void {
  fs.mkdirSync(PRR_D01C_SCREENSHOT_DIR, { recursive: true })
}

export function prrD01cScreenshotPath(filename: string): string {
  return path.join(PRR_D01C_SCREENSHOT_DIR, filename)
}

export async function capturePrrD01cScreenshot(page: Page, filename: string): Promise<string> {
  ensurePrrD01cEvidenceDirs()
  const target = prrD01cScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function capturePrrD01cLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensurePrrD01cEvidenceDirs()
  const target = prrD01cScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** IBL-C2 / F18 side-by-side rendered PDF compare — 1920×1080 dual-brand evidence. */
export const IBL_C2_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'IBL-C2')
export const IBL_C2_SCREENSHOT_DIR = path.join(IBL_C2_EVIDENCE_ROOT, 'screenshots')
export const IBL_C2_VIEWPORT = { width: 1920, height: 1080 } as const
export const IBL_C2_NARROW_VIEWPORT = { width: 900, height: 900 } as const

export function ensureIblC2EvidenceDirs(): void {
  fs.mkdirSync(IBL_C2_SCREENSHOT_DIR, { recursive: true })
}

export function iblC2ScreenshotPath(filename: string): string {
  return path.join(IBL_C2_SCREENSHOT_DIR, filename)
}

export async function captureIblC2Screenshot(page: Page, filename: string): Promise<string> {
  ensureIblC2EvidenceDirs()
  const target = iblC2ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureIblC2LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureIblC2EvidenceDirs()
  const target = iblC2ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** IBL-E1 / #128 locale-variant model UI — 1440×900 dual-brand evidence. */
export const IBL_E1_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'IBL-E1')
export const IBL_E1_SCREENSHOT_DIR = path.join(IBL_E1_EVIDENCE_ROOT, 'screenshots')
export const IBL_E1_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureIblE1EvidenceDirs(): void {
  fs.mkdirSync(IBL_E1_SCREENSHOT_DIR, { recursive: true })
}

export function iblE1ScreenshotPath(filename: string): string {
  return path.join(IBL_E1_SCREENSHOT_DIR, filename)
}

export async function captureIblE1Screenshot(page: Page, filename: string): Promise<string> {
  ensureIblE1EvidenceDirs()
  const target = iblE1ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureIblE1LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureIblE1EvidenceDirs()
  const target = iblE1ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

export const NAV_MISSING_ICONS_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'nav-missing-icons',
)
export const NAV_MISSING_ICONS_SCREENSHOT_DIR = path.join(
  NAV_MISSING_ICONS_EVIDENCE_ROOT,
  'screenshots',
)
export const NAV_MISSING_ICONS_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureNavMissingIconsEvidenceDirs(): void {
  fs.mkdirSync(NAV_MISSING_ICONS_SCREENSHOT_DIR, { recursive: true })
}

export function navMissingIconsScreenshotPath(filename: string): string {
  return path.join(NAV_MISSING_ICONS_SCREENSHOT_DIR, filename)
}

export async function captureNavMissingIconsScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureNavMissingIconsEvidenceDirs()
  const target = navMissingIconsScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureNavMissingIconsLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureNavMissingIconsEvidenceDirs()
  const target = navMissingIconsScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** TM #144 published-template-test-artifacts — release Testing read-only @1920. */
export const TM144_PTA_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'TM144-published-template-test-artifacts',
)
export const TM144_PTA_SCREENSHOT_DIR = path.join(TM144_PTA_EVIDENCE_ROOT, 'screenshots')
export const TM144_PTA_VIEWPORT = { width: 1920, height: 1080 } as const

export function ensureTm144PtaEvidenceDirs(): void {
  fs.mkdirSync(TM144_PTA_SCREENSHOT_DIR, { recursive: true })
}

export function tm144PtaScreenshotPath(filename: string): string {
  return path.join(TM144_PTA_SCREENSHOT_DIR, filename)
}

export async function captureTm144PtaScreenshot(page: Page, filename: string): Promise<string> {
  ensureTm144PtaEvidenceDirs()
  const target = tm144PtaScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureTm144PtaLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureTm144PtaEvidenceDirs()
  const target = tm144PtaScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** SYS-NORM Wave 1 — shell fluid + nav trim + EditMore + EntityLink — 1440×900 dual-brand. */
export const SYS_NORM_W1_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'SYS-NORM-W1')
export const SYS_NORM_W1_SCREENSHOT_DIR = path.join(SYS_NORM_W1_EVIDENCE_ROOT, 'screenshots')
export const SYS_NORM_W1_VIEWPORT = { width: 1440, height: 900 } as const
export const SYS_NORM_W1_WIDE_VIEWPORT = { width: 1800, height: 900 } as const

export function ensureSysNormW1EvidenceDirs(): void {
  fs.mkdirSync(SYS_NORM_W1_SCREENSHOT_DIR, { recursive: true })
}

export function sysNormW1ScreenshotPath(filename: string): string {
  return path.join(SYS_NORM_W1_SCREENSHOT_DIR, filename)
}

export async function captureSysNormW1Screenshot(page: Page, filename: string): Promise<string> {
  ensureSysNormW1EvidenceDirs()
  const target = sysNormW1ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureSysNormW1LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureSysNormW1EvidenceDirs()
  const target = sysNormW1ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** SYS-NORM Wave 2 — Template/Master Package Hub IA — 1440×900 dual-brand. */
export const SYS_NORM_W2_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'SYS-NORM-W2')
export const SYS_NORM_W2_SCREENSHOT_DIR = path.join(SYS_NORM_W2_EVIDENCE_ROOT, 'screenshots')
export const SYS_NORM_W2_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureSysNormW2EvidenceDirs(): void {
  fs.mkdirSync(SYS_NORM_W2_SCREENSHOT_DIR, { recursive: true })
}

export function sysNormW2ScreenshotPath(filename: string): string {
  return path.join(SYS_NORM_W2_SCREENSHOT_DIR, filename)
}

export async function captureSysNormW2Screenshot(page: Page, filename: string): Promise<string> {
  ensureSysNormW2EvidenceDirs()
  const target = sysNormW2ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureSysNormW2LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureSysNormW2EvidenceDirs()
  const target = sysNormW2ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** SYS-NORM Wave 3 — External services ops (dashboard / invocations / settings) — 1440×900 dual-brand. */
export const SYS_NORM_W3_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'SYS-NORM-W3')
export const SYS_NORM_W3_SCREENSHOT_DIR = path.join(SYS_NORM_W3_EVIDENCE_ROOT, 'screenshots')
export const SYS_NORM_W3_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureSysNormW3EvidenceDirs(): void {
  fs.mkdirSync(SYS_NORM_W3_SCREENSHOT_DIR, { recursive: true })
}

export function sysNormW3ScreenshotPath(filename: string): string {
  return path.join(SYS_NORM_W3_SCREENSHOT_DIR, filename)
}

export async function captureSysNormW3Screenshot(page: Page, filename: string): Promise<string> {
  ensureSysNormW3EvidenceDirs()
  const target = sysNormW3ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureSysNormW3LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureSysNormW3EvidenceDirs()
  const target = sysNormW3ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** SYS-NORM Wave 5 — Six-role compression (picker / journeys / remapped roles) — 1440×900 dual-brand. */
export const SYS_NORM_W5_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'SYS-NORM-W5')
export const SYS_NORM_W5_SCREENSHOT_DIR = path.join(SYS_NORM_W5_EVIDENCE_ROOT, 'screenshots')
export const SYS_NORM_W5_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureSysNormW5EvidenceDirs(): void {
  fs.mkdirSync(SYS_NORM_W5_SCREENSHOT_DIR, { recursive: true })
}

export function sysNormW5ScreenshotPath(filename: string): string {
  return path.join(SYS_NORM_W5_SCREENSHOT_DIR, filename)
}

export async function captureSysNormW5Screenshot(page: Page, filename: string): Promise<string> {
  ensureSysNormW5EvidenceDirs()
  const target = sysNormW5ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureSysNormW5LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureSysNormW5EvidenceDirs()
  const target = sysNormW5ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** SYS-NORM Wave 7 — Templates Import dry-run dialog — 1440×900 dual-brand. */
export const SYS_NORM_W7_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'SYS-NORM-W7')
export const SYS_NORM_W7_SCREENSHOT_DIR = path.join(SYS_NORM_W7_EVIDENCE_ROOT, 'screenshots')
export const SYS_NORM_W7_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureSysNormW7EvidenceDirs(): void {
  fs.mkdirSync(SYS_NORM_W7_SCREENSHOT_DIR, { recursive: true })
}

export function sysNormW7ScreenshotPath(filename: string): string {
  return path.join(SYS_NORM_W7_SCREENSHOT_DIR, filename)
}

export async function captureSysNormW7Screenshot(page: Page, filename: string): Promise<string> {
  ensureSysNormW7EvidenceDirs()
  const target = sysNormW7ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureSysNormW7LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureSysNormW7EvidenceDirs()
  const target = sysNormW7ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** Reminder timing settings IA / #153 — System settings page + Team settings dialog @1440. */
export const REMINDER_TIMING_IA_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'reminder-timing-settings-ia',
)
export const REMINDER_TIMING_IA_SCREENSHOT_DIR = path.join(
  REMINDER_TIMING_IA_EVIDENCE_ROOT,
  'screenshots',
)
export const REMINDER_TIMING_IA_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureReminderTimingIaEvidenceDirs(): void {
  fs.mkdirSync(REMINDER_TIMING_IA_SCREENSHOT_DIR, { recursive: true })
}

export function reminderTimingIaScreenshotPath(filename: string): string {
  return path.join(REMINDER_TIMING_IA_SCREENSHOT_DIR, filename)
}

export async function captureReminderTimingIaScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureReminderTimingIaEvidenceDirs()
  const target = reminderTimingIaScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureReminderTimingIaLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureReminderTimingIaEvidenceDirs()
  const target = reminderTimingIaScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** Asset library group isolation / #154 — list filter + upload group @1440 dual-brand. */
export const ASSET_LIBRARY_GROUP_ISOLATION_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'asset-library-group-isolation',
)
export const ASSET_LIBRARY_GROUP_ISOLATION_SCREENSHOT_DIR = path.join(
  ASSET_LIBRARY_GROUP_ISOLATION_EVIDENCE_ROOT,
  'screenshots',
)
export const ASSET_LIBRARY_GROUP_ISOLATION_VIEWPORT = { width: 1440, height: 900 } as const

export function ensureAssetLibraryGroupIsolationEvidenceDirs(): void {
  fs.mkdirSync(ASSET_LIBRARY_GROUP_ISOLATION_SCREENSHOT_DIR, { recursive: true })
}

export function assetLibraryGroupIsolationScreenshotPath(filename: string): string {
  return path.join(ASSET_LIBRARY_GROUP_ISOLATION_SCREENSHOT_DIR, filename)
}

export async function captureAssetLibraryGroupIsolationScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureAssetLibraryGroupIsolationEvidenceDirs()
  const target = assetLibraryGroupIsolationScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureAssetLibraryGroupIsolationLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureAssetLibraryGroupIsolationEvidenceDirs()
  const target = assetLibraryGroupIsolationScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** Binding editor IA / #155+#156 — sticky rail, compact toolbar, preview @1920 dual-brand. */
export const BINDING_EDITOR_IA_EVIDENCE_ROOT = path.join(
  E2E_DIR,
  '..',
  'evidence',
  'binding-editor-ia',
)
export const BINDING_EDITOR_IA_SCREENSHOT_DIR = path.join(
  BINDING_EDITOR_IA_EVIDENCE_ROOT,
  'screenshots',
)
/** BDD-BEI-020 / BEI-C15: dual-brand UIUX target viewport. */
export const BINDING_EDITOR_IA_VIEWPORT = { width: 1920, height: 1080 } as const
export const BINDING_EDITOR_IA_DESKTOP_VIEWPORT = { width: 1440, height: 900 } as const
export const BINDING_EDITOR_IA_NARROW_VIEWPORT = { width: 375, height: 812 } as const

export function ensureBindingEditorIaEvidenceDirs(): void {
  fs.mkdirSync(BINDING_EDITOR_IA_SCREENSHOT_DIR, { recursive: true })
}

export function bindingEditorIaScreenshotPath(filename: string): string {
  return path.join(BINDING_EDITOR_IA_SCREENSHOT_DIR, filename)
}

export async function captureBindingEditorIaScreenshot(
  page: Page,
  filename: string,
): Promise<string> {
  ensureBindingEditorIaEvidenceDirs()
  const target = bindingEditorIaScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function captureBindingEditorIaLocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensureBindingEditorIaEvidenceDirs()
  const target = bindingEditorIaScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

/** PQH N22 / #162 - Catalog Edit/More row actions - 1440x900 dual-brand. */
export const PQH_N22_EVIDENCE_ROOT = path.join(E2E_DIR, '..', 'evidence', 'PQH-N22')
export const PQH_N22_SCREENSHOT_DIR = path.join(PQH_N22_EVIDENCE_ROOT, 'screenshots')
export const PQH_N22_VIEWPORT = { width: 1440, height: 900 } as const

export function ensurePqhN22EvidenceDirs(): void {
  fs.mkdirSync(PQH_N22_SCREENSHOT_DIR, { recursive: true })
}

export function pqhN22ScreenshotPath(filename: string): string {
  return path.join(PQH_N22_SCREENSHOT_DIR, filename)
}

export async function capturePqhN22Screenshot(page: Page, filename: string): Promise<string> {
  ensurePqhN22EvidenceDirs()
  const target = pqhN22ScreenshotPath(filename)
  await page.screenshot({ path: target, fullPage: false })
  return filename
}

export async function capturePqhN22LocatorScreenshot(
  locator: Locator,
  filename: string,
): Promise<string> {
  ensurePqhN22EvidenceDirs()
  const target = pqhN22ScreenshotPath(filename)
  await locator.screenshot({ path: target })
  return filename
}

