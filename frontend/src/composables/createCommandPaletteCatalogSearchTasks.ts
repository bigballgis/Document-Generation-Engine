import { listContentModules } from '@/api/contentModules'
import { listMasters } from '@/api/masters'
import { listTemplates } from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import { COMMAND_PALETTE_PAGE_SIZE, type PaletteItem } from '@/composables/commandPaletteTypes'
import { canQueryCatalog } from '@/composables/commandPaletteHelpers'
import {
  contentModuleDetailPath,
  masterDetailPath,
  ROUTE_KEYS,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import type { Ref } from 'vue'

export function createCommandPaletteCatalogSearchTasks(options: {
  routes: readonly string[]
  trimmed: string
  signal: AbortSignal
  templateItems: Ref<PaletteItem[]>
  masterItems: Ref<PaletteItem[]>
  contentModuleItems: Ref<PaletteItem[]>
  templateErrorKey: Ref<string | null>
  masterErrorKey: Ref<string | null>
  contentModuleErrorKey: Ref<string | null>
  templateLoading: Ref<boolean>
  masterLoading: Ref<boolean>
  contentModuleLoading: Ref<boolean>
}): Array<Promise<void>> {
  const {
    routes,
    trimmed,
    signal,
    templateItems,
    masterItems,
    contentModuleItems,
    templateErrorKey,
    masterErrorKey,
    contentModuleErrorKey,
    templateLoading,
    masterLoading,
    contentModuleLoading,
  } = options

  const tasks: Array<Promise<void>> = []

  if (canQueryCatalog(routes, ROUTE_KEYS.templateManagement)) {
    templateLoading.value = true
    templateErrorKey.value = null
    tasks.push(
      listTemplates(0, COMMAND_PALETTE_PAGE_SIZE, { search: trimmed, signal })
        .then((page) => {
          if (signal.aborted) {
            return
          }
          templateItems.value = page.content.map((row) => ({
            id: `template:${row.id}`,
            kind: 'template' as const,
            title: row.name,
            subtitle: [row.externalId, row.groupCode].filter(Boolean).join(' · '),
            target: { path: templatePackageHubPath(row.id) },
          }))
          templateErrorKey.value = null
        })
        .catch((error: unknown) => {
          if (signal.aborted) {
            return
          }
          templateItems.value = []
          templateErrorKey.value = resolveApiErrorMessageKey(
            error,
            'commandPalette.errors.templates',
          )
        })
        .finally(() => {
          if (!signal.aborted) {
            templateLoading.value = false
          }
        }),
    )
  } else {
    templateItems.value = []
    templateErrorKey.value = null
    templateLoading.value = false
  }

  if (canQueryCatalog(routes, ROUTE_KEYS.masterManagement)) {
    masterLoading.value = true
    masterErrorKey.value = null
    tasks.push(
      listMasters(0, COMMAND_PALETTE_PAGE_SIZE, { search: trimmed, signal })
        .then((page) => {
          if (signal.aborted) {
            return
          }
          masterItems.value = page.content.map((row) => ({
            id: `master:${row.id}`,
            kind: 'master' as const,
            title: row.name,
            subtitle: row.groupCode,
            target: { path: masterDetailPath(row.id) },
          }))
          masterErrorKey.value = null
        })
        .catch((error: unknown) => {
          if (signal.aborted) {
            return
          }
          masterItems.value = []
          masterErrorKey.value = resolveApiErrorMessageKey(error, 'commandPalette.errors.masters')
        })
        .finally(() => {
          if (!signal.aborted) {
            masterLoading.value = false
          }
        }),
    )
  } else {
    masterItems.value = []
    masterErrorKey.value = null
    masterLoading.value = false
  }

  if (canQueryCatalog(routes, ROUTE_KEYS.contentModuleManagement)) {
    contentModuleLoading.value = true
    contentModuleErrorKey.value = null
    tasks.push(
      listContentModules(0, COMMAND_PALETTE_PAGE_SIZE, { search: trimmed, signal })
        .then((page) => {
          if (signal.aborted) {
            return
          }
          contentModuleItems.value = page.content.map((row) => ({
            id: `content-module:${row.moduleId}`,
            kind: 'content-module' as const,
            title: row.name,
            subtitle: row.moduleCode,
            target: { path: contentModuleDetailPath(row.moduleId) },
          }))
          contentModuleErrorKey.value = null
        })
        .catch((error: unknown) => {
          if (signal.aborted) {
            return
          }
          contentModuleItems.value = []
          contentModuleErrorKey.value = resolveApiErrorMessageKey(
            error,
            'commandPalette.errors.contentModules',
          )
        })
        .finally(() => {
          if (!signal.aborted) {
            contentModuleLoading.value = false
          }
        }),
    )
  } else {
    contentModuleItems.value = []
    contentModuleErrorKey.value = null
    contentModuleLoading.value = false
  }

  return tasks
}
