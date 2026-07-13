import { defineStore } from 'pinia'
import { ref } from 'vue'
import { createIdentityGroupActions } from '@/stores/createIdentityGroupActions'
import { createIdentityUserActions } from '@/stores/createIdentityUserActions'
import type {
  BusinessGroupView,
  GroupQuery,
  ManagementUserView,
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
  const lastUserErrorRetryable = ref(false)
  const lastGroupErrorMessageKey = ref<string | null>(null)
  const lastGroupErrorRetryable = ref(false)

  const userActions = createIdentityUserActions({
    users,
    usersTotal,
    userFilters,
    loadingUsers,
    submitting,
    lastUserErrorMessageKey,
    lastUserErrorRetryable,
  })

  const groupActions = createIdentityGroupActions({
    groups,
    groupsTotal,
    groupFilters,
    loadingGroups,
    submitting,
    lastGroupErrorMessageKey,
    lastGroupErrorRetryable,
  })

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
    lastUserErrorRetryable,
    lastGroupErrorMessageKey,
    lastGroupErrorRetryable,
    ...userActions,
    ...groupActions,
  }
})
