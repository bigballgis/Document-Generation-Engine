import type { ComputedRef, Ref } from 'vue'
import type { ManagementCapabilities } from '@/types/session'

/** Locked by BDD C6-C6 / Vitest. */
export const COMMAND_PALETTE_PAGE_SIZE = 8

/** Locked by BDD C6-C7 / Vitest. */
export const COMMAND_PALETTE_DEBOUNCE_MS = 250

export type PaletteItemKind = 'route' | 'template' | 'master' | 'content-module'

export interface PaletteNavTarget {
  path: string
  query?: Record<string, string>
  hash?: string
}

export interface PaletteItem {
  id: string
  kind: PaletteItemKind
  title: string
  subtitle: string
  target: PaletteNavTarget
}

export interface PaletteGroupView {
  id: 'routes' | 'templates' | 'masters' | 'content-modules'
  labelKey: string
  items: PaletteItem[]
  errorMessageKey: string | null
  loading: boolean
}

export type ReadonlyStringList = Ref<readonly string[]> | ComputedRef<readonly string[]>
export type CapabilitiesRef =
  | Ref<ManagementCapabilities | undefined>
  | ComputedRef<ManagementCapabilities | undefined>

export interface UseCommandPaletteOptions {
  visibleRoutes: ReadonlyStringList
  roles: ReadonlyStringList
  capabilities: CapabilitiesRef
  translate: (key: string) => string
  navigate: (target: PaletteNavTarget) => void | Promise<void>
  /** Injected for tests; defaults to document keydown. */
  bindShortcut?: boolean
}
