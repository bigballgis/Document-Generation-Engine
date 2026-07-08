<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { rowSortMethod } from '@/composables/useDataTableFilters'
import { canManageGroups } from '@/auth/identityRoles'
import { useIdentityStore } from '@/stores/identity'
import { useSessionStore } from '@/stores/session'
import type { BusinessGroupView, CreateGroupRequest, GroupDimension } from '@/types/identity'

const { t, te } = useI18n()
const identityStore = useIdentityStore()
const sessionStore = useSessionStore()

const canManage = computed(() => canManageGroups(sessionStore.session?.roles ?? []))

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<{ groupCode: string; displayName: string; dimension: GroupDimension | '' }>({
  groupCode: '',
  displayName: '',
  dimension: '',
})

const currentPage = ref(1)
const searchQuery = ref('')

const filteredGroups = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) {
    return identityStore.groups
  }
  return identityStore.groups.filter(
    (group) =>
      group.groupCode.toLowerCase().includes(query) ||
      group.displayName.toLowerCase().includes(query),
  )
})

const dimensionOptions: GroupDimension[] = ['BUSINESS_LINE', 'DEPARTMENT']

function dimensionLabel(dimension: string): string {
  return te(`identity.dimensions.${dimension}`) ? t(`identity.dimensions.${dimension}`) : dimension
}

type GroupMoreAction = 'toggleEnabled'

function handleMoreAction(command: GroupMoreAction, group: BusinessGroupView) {
  if (command === 'toggleEnabled') {
    void toggleEnabled(group)
  }
}

const errorMessage = computed(() => {
  const key = identityStore.lastGroupErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('identity.error.loadGroups')
})

const formRules = computed<FormRules>(() => ({
  groupCode: [
    { required: true, message: t('identity.groups.validation.groupCodeRequired'), trigger: 'blur' },
    {
      pattern: /^[A-Z0-9_]{1,64}$/,
      message: t('identity.groups.validation.groupCodePattern'),
      trigger: 'blur',
    },
  ],
  displayName: [
    { required: true, message: t('identity.groups.validation.displayNameRequired'), trigger: 'blur' },
  ],
  dimension: [
    { required: true, message: t('identity.groups.validation.dimensionRequired'), trigger: 'change' },
  ],
}))

onMounted(() => {
  void reload()
})

watch(currentPage, (page) => {
  void identityStore.fetchGroups({ page: page - 1, size: identityStore.groupFilters.size }).catch(() => {
    // Surfaced via store error key.
  })
})

async function reload() {
  currentPage.value = 1
  try {
    await identityStore.fetchGroups({ page: 0 })
  } catch {
    // Surfaced via store error key.
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  form.groupCode = ''
  form.displayName = ''
  form.dimension = ''
  dialogVisible.value = true
}

function openEdit(group: BusinessGroupView) {
  dialogMode.value = 'edit'
  editingId.value = group.id
  form.groupCode = group.groupCode
  form.displayName = group.displayName
  form.dimension = group.dimension
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  try {
    if (dialogMode.value === 'create' && form.dimension) {
      const payload: CreateGroupRequest = {
        groupCode: form.groupCode,
        displayName: form.displayName,
        dimension: form.dimension,
      }
      await identityStore.createGroup(payload)
      ElMessage.success(t('identity.groups.createSuccess'))
    } else if (editingId.value) {
      await identityStore.updateGroup(editingId.value, form.displayName)
      ElMessage.success(t('identity.groups.updateSuccess'))
    }
    dialogVisible.value = false
  } catch {
    ElMessage.error(errorMessage.value || t('identity.error.createGroup'))
  }
}

async function toggleEnabled(group: BusinessGroupView) {
  try {
    if (group.enabled) {
      await ElMessageBox.confirm(
        t('identity.groups.confirmDisableMessage', { groupCode: group.groupCode }),
        t('identity.groups.confirmDisableTitle'),
        { type: 'warning' },
      )
    }
    await identityStore.setGroupEnabled(group.id, !group.enabled)
    ElMessage.success(
      group.enabled ? t('identity.groups.disableSuccess') : t('identity.groups.enableSuccess'),
    )
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(errorMessage.value || t('identity.error.updateGroup'))
  }
}

const sortByDimension = rowSortMethod<BusinessGroupView>((row) => row.dimension)
const sortByEnabled = rowSortMethod<BusinessGroupView>((row) => row.enabled)
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
      v-if="errorMessage && !identityStore.loadingGroups"
      :message-key="identityStore.lastGroupErrorMessageKey ?? 'identity.error.loadGroups'"
      :retryable="identityStore.lastGroupErrorRetryable"
      @retry="reload"
    />

    <el-alert
      v-else-if="errorMessage"
      class="panel-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="identityStore.loadingGroups" :rows="5" animated />

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
            <el-button link size="small" type="primary" @click="openEdit(row)">
              {{ t('identity.groups.edit') }}
            </el-button>
            <el-dropdown trigger="click" @command="(command: GroupMoreAction) => handleMoreAction(command, row)">
              <el-button link size="small">
                {{ t('common.more') }}
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="toggleEnabled">
                    {{ row.enabled ? t('identity.groups.disable') : t('identity.groups.enable') }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </AppDataTable>

        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="identityStore.groupFilters.size ?? 20"
          :total="identityStore.groupsTotal"
        />
      </template>

      <EmptyStatePanel v-else title-key="identity.groups.empty" />

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

<style scoped lang="scss">
.group-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.panel-header__leading {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
}

.panel-search {
  max-width: 320px;
}

.read-only-hint {
  margin: 0;
  color: var(--text-muted);
}

.panel-alert {
  margin-bottom: 0.5rem;
}

.full-width {
  width: 100%;
}
</style>
