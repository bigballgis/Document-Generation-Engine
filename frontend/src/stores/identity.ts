import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as identityApi from '@/api/identity'
import { resolveApiErrorMessageKey } from '@/api/http'
import type {
  BusinessGroupView,
  CreateGroupRequest,
  CreateUserRequest,
  GroupQuery,
  ManagementUserView,
  UpdateUserRequest,
  UserQuery,
} from '@/types/identity'

const DEFAULT_PAGE_SIZE = 20

export const useIdentityStore = defineStore('identity', () => {
  const users = ref<ManagementUserView[]>([])
  const usersTotal = ref(0)
  const userFilters = ref<UserQuery>({ page: 0, size: DEFAULT_PAGE_SIZE })

  const groups = ref<BusinessGroupView[]>([])
  const groupsTotal = ref(0)
  const groupFilters = ref<GroupQuery>({ page: 0, size: DEFAULT_PAGE_SIZE })

  const loadingUsers = ref(false)
  const loadingGroups = ref(false)
  const submitting = ref(false)
  const lastUserErrorMessageKey = ref<string | null>(null)
  const lastGroupErrorMessageKey = ref<string | null>(null)

  function applyUpdatedUser(updated: ManagementUserView) {
    users.value = users.value.map((item) => (item.id === updated.id ? updated : item))
  }

  function applyUpdatedGroup(updated: BusinessGroupView) {
    groups.value = groups.value.map((item) => (item.id === updated.id ? updated : item))
  }

  async function fetchUsers(query: UserQuery = userFilters.value): Promise<void> {
    loadingUsers.value = true
    lastUserErrorMessageKey.value = null
    userFilters.value = { ...userFilters.value, ...query }
    try {
      const page = await identityApi.listUsers(userFilters.value)
      users.value = page.content
      usersTotal.value = page.totalElements
    } catch (error) {
      lastUserErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.loadUsers')
      throw error
    } finally {
      loadingUsers.value = false
    }
  }

  async function createUser(body: CreateUserRequest): Promise<ManagementUserView> {
    submitting.value = true
    lastUserErrorMessageKey.value = null
    try {
      const created = await identityApi.createUser(body)
      users.value = [created, ...users.value]
      usersTotal.value += 1
      return created
    } catch (error) {
      lastUserErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.createUser')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function updateUser(id: string, body: UpdateUserRequest): Promise<ManagementUserView> {
    submitting.value = true
    lastUserErrorMessageKey.value = null
    try {
      const updated = await identityApi.updateUser(id, body)
      applyUpdatedUser(updated)
      return updated
    } catch (error) {
      lastUserErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.updateUser')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function setUserEnabled(id: string, enabled: boolean): Promise<ManagementUserView> {
    submitting.value = true
    lastUserErrorMessageKey.value = null
    try {
      const updated = enabled ? await identityApi.enableUser(id) : await identityApi.disableUser(id)
      applyUpdatedUser(updated)
      return updated
    } catch (error) {
      lastUserErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.updateUser')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function resetUserPassword(id: string, newPassword: string): Promise<ManagementUserView> {
    submitting.value = true
    lastUserErrorMessageKey.value = null
    try {
      const updated = await identityApi.resetUserPassword(id, newPassword)
      applyUpdatedUser(updated)
      return updated
    } catch (error) {
      lastUserErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.resetPassword')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function deleteUser(id: string): Promise<void> {
    submitting.value = true
    lastUserErrorMessageKey.value = null
    try {
      await identityApi.deleteUser(id)
      users.value = users.value.filter((item) => item.id !== id)
      usersTotal.value = Math.max(0, usersTotal.value - 1)
    } catch (error) {
      lastUserErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.deleteUser')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function fetchGroups(query: GroupQuery = groupFilters.value): Promise<void> {
    loadingGroups.value = true
    lastGroupErrorMessageKey.value = null
    groupFilters.value = { ...groupFilters.value, ...query }
    try {
      const page = await identityApi.listGroups(groupFilters.value)
      groups.value = page.content
      groupsTotal.value = page.totalElements
    } catch (error) {
      lastGroupErrorMessageKey.value = resolveApiErrorMessageKey(error, 'identity.error.loadGroups')
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
    users,
    usersTotal,
    userFilters,
    groups,
    groupsTotal,
    groupFilters,
    loadingUsers,
    loadingGroups,
    submitting,
    lastUserErrorMessageKey,
    lastGroupErrorMessageKey,
    fetchUsers,
    createUser,
    updateUser,
    setUserEnabled,
    resetUserPassword,
    deleteUser,
    fetchGroups,
    createGroup,
    updateGroup,
    setGroupEnabled,
  }
})
