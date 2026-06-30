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

export async function switchBrand(page: Page, brand: BrandPreset): Promise<void> {
  const brandSwitcher = page.locator('.brand-switcher')
  await brandSwitcher.click()
  await page.locator('.el-select-dropdown__item').filter({ hasText: brand }).click()
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
