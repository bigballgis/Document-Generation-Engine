import type { Ref } from 'vue'
import * as identityApi from '@/api/identity'
import { resolveApiErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import type {
  CreateUserRequest,
  ManagementUserView,
  UpdateUserRequest,
  UserQuery,
} from '@/types/identity'

export function createIdentityUserActions(deps: {
  users: Ref<ManagementUserView[]>
  usersTotal: Ref<number>
  userFilters: Ref<UserQuery>
  loadingUsers: Ref<boolean>
  submitting: Ref<boolean>
  lastUserErrorMessageKey: Ref<string | null>
  lastUserErrorRetryable: Ref<boolean>
}) {
  const {
    users,
    usersTotal,
    userFilters,
    loadingUsers,
    submitting,
    lastUserErrorMessageKey,
    lastUserErrorRetryable,
  } = deps

  function applyUpdatedUser(updated: ManagementUserView) {
    users.value = users.value.map((item) => (item.id === updated.id ? updated : item))
  }

  async function fetchUsers(
    query: UserQuery = userFilters.value,
    options: AbortableRequestOptions = {},
  ): Promise<void> {
    loadingUsers.value = true
    clearStoreListError(lastUserErrorMessageKey, lastUserErrorRetryable)
    userFilters.value = { ...userFilters.value, ...query }
    try {
      const page = await identityApi.listUsers(userFilters.value, options)
      users.value = page.content
      usersTotal.value = page.totalElements
    } catch (error) {
      handleStoreListFailure(
        error,
        'identity.error.loadUsers',
        lastUserErrorMessageKey,
        lastUserErrorRetryable,
      )
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

  return {
    fetchUsers,
    createUser,
    updateUser,
    setUserEnabled,
    resetUserPassword,
    deleteUser,
  }
}
