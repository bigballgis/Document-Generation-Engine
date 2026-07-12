import {
  buildVisibleNavGroups,
  resolveNavItemTarget,
  type NavItemDefinition,
} from '@/navigation/navStructure'
import type { ManagementCapabilities } from '@/types/session'
import type { PaletteItem } from '@/composables/commandPaletteTypes'

function containsIgnoreCase(haystack: string, needle: string): boolean {
  return haystack.toLocaleLowerCase().includes(needle.toLocaleLowerCase())
}

function navItemToPaletteItem(
  navItem: NavItemDefinition,
  translate: (key: string) => string,
): PaletteItem {
  const target = resolveNavItemTarget(navItem)
  return {
    id: `route:${navItem.id}`,
    kind: 'route',
    title: translate(navItem.labelKey),
    subtitle: target.path,
    target,
  }
}

/** Flatten sidebar-aligned nav items from visibleRoutes (legacy keys deduped via path). */
export function buildPaletteRouteItems(
  visibleRoutes: readonly string[],
  roles: readonly string[],
  capabilities: ManagementCapabilities | undefined,
  translate: (key: string) => string,
): PaletteItem[] {
  const groups = buildVisibleNavGroups([...visibleRoutes], [...roles], capabilities)
  const seenPaths = new Set<string>()
  const items: PaletteItem[] = []
  for (const group of groups) {
    for (const navItem of group.items) {
      const target = resolveNavItemTarget(navItem)
      const pathKey = `${target.path}|${target.hash ?? ''}|${JSON.stringify(target.query ?? {})}`
      if (seenPaths.has(pathKey)) {
        continue
      }
      seenPaths.add(pathKey)
      items.push(navItemToPaletteItem(navItem, translate))
    }
  }
  return items
}

export function filterPaletteRouteItems(items: PaletteItem[], query: string): PaletteItem[] {
  const trimmed = query.trim()
  if (!trimmed) {
    return items
  }
  return items.filter(
    (item) =>
      containsIgnoreCase(item.title, trimmed) || containsIgnoreCase(item.subtitle, trimmed),
  )
}

/** Clamp highlight (no wrap) — BDD C6-C12. */
export function moveHighlightIndex(current: number, delta: number, length: number): number {
  if (length <= 0) {
    return -1
  }
  const next = current < 0 ? (delta > 0 ? 0 : length - 1) : current + delta
  return Math.max(0, Math.min(length - 1, next))
}

export function canQueryCatalog(visibleRoutes: readonly string[], routeKey: string): boolean {
  return visibleRoutes.includes(routeKey)
}
