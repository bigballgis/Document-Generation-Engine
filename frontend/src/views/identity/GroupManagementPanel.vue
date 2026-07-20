<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import TableEditMoreActions from '@/components/common/TableEditMoreActions.vue'
import {
  useGroupManagementPanel,
  type GroupMoreAction,
} from '@/views/identity/useGroupManagementPanel'

const {
  t,
  identityStore,
  canManage,
  dialogVisible,
  dialogMode,
  formRef,
  form,
  currentPage,
  searchQuery,
  filteredGroups,
  dimensionOptions,
  dimensionLabel,
  handleMoreAction,
  errorMessage,
  formRules,
  reload,
  openCreate,
  openEdit,
  submitForm,
  sortByDimension,
  sortByEnabled,
} = useGroupManagementPanel()
</script>

<template>
  <section class="group-panel">
    <header class="panel-header">
      <div class="panel-header__leading">
        <p v-if="!canManage" class="read-only-hint">{{ t('identity.groups.readOnlyHint') }}</p>
        <el-input
          v-model="searchQuery"
          class="panel-search"
          clearable
          :placeholder="t('identity.groups.searchPlaceholder')"
          :prefix-icon="Search"
        />
      </div>
      <el-button v-if="canManage" type="primary" @click="openCreate">
        {{ t('identity.groups.create') }}
      </el-button>
    </header>

    <LoadErrorPanel
      v-if="errorMessage"
      :message-key="identityStore.lastGroupErrorMessageKey ?? 'identity.error.loadGroups'"
      :retryable="identityStore.lastGroupErrorRetryable"
      @retry="reload"
    />

    <el-skeleton v-else-if="identityStore.loadingGroups" :rows="5" animated />

    <template v-else>
      <template v-if="identityStore.groups.length > 0">
        <AppDataTable :data="filteredGroups">
        <el-table-column prop="groupCode" sortable min-width="160" :label="t('identity.groups.columns.groupCode')" />
        <el-table-column prop="displayName" sortable min-width="200" :label="t('identity.groups.columns.displayName')" />
        <el-table-column
          sortable
          :sort-method="sortByDimension"
          width="160"
          :label="t('identity.groups.columns.dimension')"
        >
          <template #default="{ row }">
            {{ dimensionLabel(row.dimension) }}
          </template>
        </el-table-column>
        <el-table-column
          sortable
          :sort-method="sortByEnabled"
          width="120"
          :label="t('identity.groups.columns.status')"
        >
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? t('identity.status.enabled') : t('identity.status.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canManage" :label="t('identity.groups.columns.actions')" width="160">
          <template #default="{ row }">
            <TableEditMoreActions
              :edit-label="t('identity.groups.edit')"
              @edit="openEdit(row)"
              @command="(command) => handleMoreAction(command as GroupMoreAction, row)"
            >
              <template #more>
                <el-dropdown-menu>
                  <el-dropdown-item command="toggleEnabled">
                    {{ row.enabled ? t('identity.groups.disable') : t('identity.groups.enable') }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </TableEditMoreActions>
          </template>
        </el-table-column>
      </AppDataTable>

        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="identityStore.groupFilters.size ?? 20"
          :total="identityStore.groupsTotal"
        />
      </template>

      <EmptyStatePanel v-else title-key="identity.groups.empty">
        <template v-if="canManage" #actions>
          <el-button type="primary" @click="openCreate">
            {{ t('identity.groups.create') }}
          </el-button>
        </template>
      </EmptyStatePanel>

    </template>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('identity.groups.createTitle') : t('identity.groups.editTitle')"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item :label="t('identity.groups.form.groupCode')" prop="groupCode">
          <el-input
            v-model="form.groupCode"
            maxlength="64"
            :disabled="dialogMode === 'edit'"
            :placeholder="t('identity.groups.form.groupCodePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('identity.groups.form.displayName')" prop="displayName">
          <el-input v-model="form.displayName" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('identity.groups.form.dimension')" prop="dimension">
          <AppSearchSelect
            v-model="form.dimension"
            class="full-width"
            :disabled="dialogMode === 'edit'"
            :placeholder="t('identity.groups.form.dimensionPlaceholder')"
          >
            <el-option
              v-for="dimension in dimensionOptions"
              :key="dimension"
              :label="dimensionLabel(dimension)"
              :value="dimension"
            />
          </AppSearchSelect>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('identity.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="identityStore.submitting" @click="submitForm">
          {{ t('identity.actions.save') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss" src="./GroupManagementPanel.scss"></style>
