import type { Ref } from 'vue'
import * as identityApi from '@/api/identity'
import { resolveApiErrorMessageKey } from '@/api/http'
import type {
  BusinessGroupView,
  CreateGroupRequest,
  GroupQuery,
} from '@/types/identity'

export function createIdentityGroupActions(deps: {
  groups: Ref<BusinessGroupView[]>
  groupsTotal: Ref<number>
  groupFilters: Ref<GroupQuery>
  loadingGroups: Ref<boolean>
  submitting: Ref<boolean>
  lastGroupErrorMessageKey: Ref<string | null>
  lastGroupErrorRetryable: Ref<boolean>
}) {
  const {
    groups,
    groupsTotal,
    groupFilters,
    loadingGroups,
    submitting,
    lastGroupErrorMessageKey,
    lastGroupErrorRetryable,
  } = deps

  function applyUpdatedGroup(updated: BusinessGroupView) {
    groups.value = groups.value.map((item) => (item.id === updated.id ? updated : item))
  }

  async function fetchGroups(query: GroupQuery = groupFilters.value): Promise<void> {
    loadingGroups.value = true
    lastGroupErrorMessageKey.value = null
    lastGroupErrorRetryable.value = false
    groupFilters.value = { ...groupFilters.value, ...query }
    try {
      const page = await identityApi.listGroups(groupFilters.value)
      groups.value = page.content
      groupsTotal.value = page.totalElements
    } catch (error) {
      lastGroupErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.loadGroups')
      lastGroupErrorRetryable.value = true
      throw error
    } finally {
      loadingGroups.value = false
    }
  }

  async function createGroup(body: CreateGroupRequest): Promise<BusinessGroupView> {
    submitting.value = true
    lastGroupErrorMessageKey.value = null
    try {
      const created = await identityApi.createGroup(body)
      groups.value = [created, ...groups.value]
      groupsTotal.value += 1
      return created
    } catch (error) {
      lastGroupErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.createGroup')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function updateGroup(id: string, displayName: string): Promise<BusinessGroupView> {
    submitting.value = true
    lastGroupErrorMessageKey.value = null
    try {
      const updated = await identityApi.updateGroup(id, displayName)
      applyUpdatedGroup(updated)
      return updated
    } catch (error) {
      lastGroupErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.updateGroup')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function setGroupEnabled(id: string, enabled: boolean): Promise<BusinessGroupView> {
    submitting.value = true
    lastGroupErrorMessageKey.value = null
    try {
      const updated = enabled
        ? await identityApi.enableGroup(id)
        : await identityApi.disableGroup(id)
      applyUpdatedGroup(updated)
      return updated
    } catch (error) {
      lastGroupErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.updateGroup')
      throw error
    } finally {
      submitting.value = false
    }
  }

  return {
    fetchGroups,
    createGroup,
    updateGroup,
    setGroupEnabled,
  }
}
