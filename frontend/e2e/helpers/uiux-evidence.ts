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
export const P14_T02_VIEWPORT = P14_T01_VIEWPORT
export const P14_T03_VIEWPORT = P14_T01_VIEWPORT
export const P18_T10_VIEWPORT = P14_T01_VIEWPORT
export const P21_T01_VIEWPORT = P14_T01_VIEWPORT
export const P21_T01A_VIEWPORT = P14_T01_VIEWPORT

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

const BRAND_OPTION_NAME: Record<BrandPreset, RegExp> = {
  REDBC: /^red bank$/i,
  GREENBC: /^green bank$/i,
}

export async function switchBrand(page: Page, brand: BrandPreset): Promise<void> {
  const brandSwitcher = page.locator('.brand-switcher')
  await brandSwitcher.click()
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
export const P21_T01B_VIEWPORT = P14_T01_VIEWPORT

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
export const P21_T03_VIEWPORT = P14_T01_VIEWPORT

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
export const P21_T04_VIEWPORT = P14_T01_VIEWPORT

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
export const P21_T05_VIEWPORT = P14_T01_VIEWPORT

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
export const P21_T08_VIEWPORT = P14_T01_VIEWPORT

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
export const P21_T09_VIEWPORT = P14_T01_VIEWPORT

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
export const P12_AUD_B10_VIEWPORT = P14_T01_VIEWPORT

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
export const P12_RISK_PROMPT_UX_VIEWPORT = P14_T01_VIEWPORT

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
export const P2_T06_VIEWPORT = P14_T01_VIEWPORT

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
export const DEMO_FULL_FLOW_VIEWPORT = P14_T01_VIEWPORT

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
export const P12_TEMPLATE_TESTING_OVERHAUL_VIEWPORT = P14_T01_VIEWPORT

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
export const LRP_B6_VIEWPORT = P14_T01_VIEWPORT

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
export const P12_API_PACKAGE_ACCESS_VIEWPORT = P14_T01_VIEWPORT

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
export const P13_EXTERNAL_SERVICES_VIEWPORT = P14_T01_VIEWPORT

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
export const UX_ENTITY_DISPLAY_VIEWPORT = P14_T01_VIEWPORT

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
export const LRP_C9_VIEWPORT = P14_T01_VIEWPORT

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
export const F7_VIEWPORT = P14_T01_VIEWPORT
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
