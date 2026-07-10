<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import { rowSortMethod } from '@/composables/useDataTableFilters'
import type { ManagementRole, ManagementUserView } from '@/types/identity'

type UserMoreAction = 'toggleEnabled' | 'resetPassword' | 'delete'

defineProps<{
  users: ManagementUserView[]
  loading: boolean
  errorMessageKey: string | null
  errorRetryable: boolean
  isFilterGroupLocked: boolean
  roleOptions: ManagementRole[]
  canCreate: boolean
  canDelete: boolean
  pageSize: number
  totalUsers: number
}>()

const emit = defineEmits<{
  'update:filterGroup': [value: string]
  'update:filterRole': [value: string]
  'update:currentPage': [value: number]
  reload: []
  resetFilters: []
  create: []
  edit: [user: ManagementUserView]
  moreAction: [command: UserMoreAction, user: ManagementUserView]
}>()

const filterGroupModel = defineModel<string>('filterGroup', { required: true })
const filterRoleModel = defineModel<string>('filterRole', { required: true })
const currentPageModel = defineModel<number>('currentPage', { required: true })

const { t, te } = useI18n()

function roleLabel(role: string): string {
  return te(`identity.roles.${role}`) ? t(`identity.roles.${role}`) : role
}

const sortUsersByEnabled = rowSortMethod<ManagementUserView>((row) => row.enabled)
</script>

<template>
  <section class="user-list-section">
    <header class="panel-header">
      <form class="filters" @submit.prevent="emit('reload')">
        <ScopedGroupSelect
          v-model="filterGroupModel"
          class="filter-control"
          :clearable="!isFilterGroupLocked"
          :placeholder="t('identity.users.filters.groupPlaceholder')"
        />
        <AppSearchSelect
          v-model="filterRoleModel"
          class="filter-control"
          clearable
          :placeholder="t('identity.users.filters.rolePlaceholder')"
        >
          <el-option
            v-for="role in roleOptions"
            :key="role"
            :label="roleLabel(role)"
            :value="role"
          />
        </AppSearchSelect>
        <el-button native-type="submit">{{ t('identity.users.filters.apply') }}</el-button>
        <el-button text @click="emit('resetFilters')">{{ t('identity.users.filters.reset') }}</el-button>
      </form>
      <el-button v-if="canCreate" type="primary" @click="emit('create')">
        {{ t('identity.users.create') }}
      </el-button>
    </header>

    <LoadErrorPanel
      v-if="errorMessageKey"
      :message-key="errorMessageKey"
      :retryable="errorRetryable"
      @retry="emit('reload')"
    />

    <el-skeleton v-else-if="loading" :rows="6" animated />

    <template v-else>
      <template v-if="users.length > 0">
        <AppDataTable :data="users">
          <el-table-column
            prop="username"
            sortable
            width="140"
            :label="t('identity.users.columns.username')"
          />
          <el-table-column
            prop="displayName"
            sortable
            min-width="160"
            :label="t('identity.users.columns.displayName')"
          />
          <el-table-column prop="email" sortable min-width="200" :label="t('identity.users.columns.email')" />
          <el-table-column min-width="200" :label="t('identity.users.columns.roles')">
            <template #default="{ row }: { row: ManagementUserView }">
              <el-tag v-for="role in row.roles" :key="role" class="role-tag" size="small">
                {{ roleLabel(role) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column min-width="160" :label="t('identity.users.columns.groups')">
            <template #default="{ row }: { row: ManagementUserView }">
              {{ row.authorizedGroupCodes.join(', ') }}
            </template>
          </el-table-column>
          <el-table-column
            sortable
            :sort-method="sortUsersByEnabled"
            width="120"
            :label="t('identity.users.columns.status')"
          >
            <template #default="{ row }: { row: ManagementUserView }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? t('identity.status.enabled') : t('identity.status.disabled') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('identity.users.columns.actions')" width="160">
            <template #default="{ row }: { row: ManagementUserView }">
              <el-button link size="small" type="primary" @click="emit('edit', row)">
                {{ t('identity.users.edit') }}
              </el-button>
              <el-dropdown
                trigger="click"
                @command="(command: UserMoreAction) => emit('moreAction', command, row)"
              >
                <el-button link size="small">
                  {{ t('common.more') }}
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="toggleEnabled">
                      {{ row.enabled ? t('identity.users.disable') : t('identity.users.enable') }}
                    </el-dropdown-item>
                    <el-dropdown-item command="resetPassword">
                      {{ t('identity.users.resetPassword') }}
                    </el-dropdown-item>
                    <el-dropdown-item v-if="canDelete" command="delete" class="delete-action">
                      {{ t('identity.users.delete') }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </AppDataTable>

        <AppTablePagination
          v-model:current-page="currentPageModel"
          :page-size="pageSize"
          :total="totalUsers"
        />
      </template>

      <EmptyStatePanel v-else title-key="identity.users.empty">
        <template v-if="canCreate" #actions>
          <el-button type="primary" @click="emit('create')">
            {{ t('identity.users.create') }}
          </el-button>
        </template>
      </EmptyStatePanel>
    </template>
  </section>
</template>

<style scoped lang="scss">
.user-list-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.filters {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
  flex-wrap: wrap;
}

.filter-control {
  flex: 1 1 12rem;
  min-width: 0;
  max-width: 20rem;
}

.role-tag {
  margin-right: 0.25rem;
}
</style>
