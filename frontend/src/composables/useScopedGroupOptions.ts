import { computed } from 'vue'
import { assignableGroupCodes, isGlobalAdmin } from '@/auth/identityRoles'
import { useIdentityStore } from '@/stores/identity'
import { useSessionStore } from '@/stores/session'

export interface ScopedGroupOption {
  value: string
  label: string
}

const DEFAULT_GROUP_PAGE_SIZE = 200

export function useScopedGroupOptions() {
  const sessionStore = useSessionStore()
  const identityStore = useIdentityStore()

  const actorRoles = computed(() => sessionStore.session?.roles ?? [])

  const isGlobalAdminActor = computed(() => isGlobalAdmin(actorRoles.value))

  const groupCatalog = computed(() => identityStore.groups.map((group) => group.groupCode))

  const authorizedGroupCodes = computed(() =>
    (sessionStore.session?.authorizedGroupCodes ?? []).filter((code) => code !== '*'),
  )

  const selectableGroupCodes = computed(() => {
    if (!sessionStore.session) {
      return []
    }
    if (isGlobalAdminActor.value) {
      return assignableGroupCodes(sessionStore.session, groupCatalog.value)
    }
    return authorizedGroupCodes.value
  })

  const groupOptions = computed<ScopedGroupOption[]>(() =>
    selectableGroupCodes.value.map((code) => ({ value: code, label: code })),
  )

  const isGroupLocked = computed(
    () => !isGlobalAdminActor.value && authorizedGroupCodes.value.length === 1,
  )

  const lockedGroupCode = computed(() =>
    isGroupLocked.value ? (authorizedGroupCodes.value[0] ?? '') : '',
  )

  async function ensureGroupCatalog(): Promise<void> {
    if (!isGlobalAdminActor.value) {
      return
    }
    if (groupCatalog.value.length > 0) {
      return
    }
    try {
      await identityStore.fetchGroups({ page: 0, size: DEFAULT_GROUP_PAGE_SIZE })
    } catch {
      // Callers surface identity store errors when needed.
    }
  }

  function resolveDefaultGroupCode(current = ''): string {
    if (isGroupLocked.value) {
      return lockedGroupCode.value
    }
    if (current.trim()) {
      return current.trim()
    }
    if (groupOptions.value.length === 1) {
      return groupOptions.value[0]?.value ?? ''
    }
    return ''
  }

  return {
    groupOptions,
    isGroupLocked,
    lockedGroupCode,
    isGlobalAdminActor,
    selectableGroupCodes,
    ensureGroupCatalog,
    resolveDefaultGroupCode,
  }
}
