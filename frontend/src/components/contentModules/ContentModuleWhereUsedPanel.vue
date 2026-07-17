<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import * as contentModulesApi from '@/api/contentModules'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import type { ContentModuleWhereUsedTemplate } from '@/types/contentModule'

const props = defineProps<{
  moduleId: string
}>()

const { t } = useI18n()
const { templateDetailLink } = useEntityLinkTargets()

const rows = ref<ContentModuleWhereUsedTemplate[]>([])
const loading = ref(false)
const loadError = ref(false)
const currentPage = ref(1)
const totalElements = ref(0)

async function loadWhereUsed() {
  if (!props.moduleId) {
    return
  }
  loading.value = true
  loadError.value = false
  try {
    const pageView = await contentModulesApi.listContentModuleWhereUsed(
      props.moduleId,
      currentPage.value - 1,
      SERVER_TABLE_PAGE_SIZE,
    )
    rows.value = pageView.content
    totalElements.value = pageView.totalElements
  } catch {
    rows.value = []
    totalElements.value = 0
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadWhereUsed()
})

watch(
  () => props.moduleId,
  () => {
    currentPage.value = 1
    void loadWhereUsed()
  },
)

watch(currentPage, () => {
  void loadWhereUsed()
})
</script>

<template>
  <section class="where-used-panel" data-testid="content-module-where-used">
    <p class="where-used-hint">{{ t('contentModules.detail.whereUsed.description') }}</p>

    <LoadErrorPanel
      v-if="loadError && !loading"
      message-key="contentModules.detail.whereUsed.loadError"
      data-testid="content-module-where-used-error"
      @retry="loadWhereUsed"
    />

    <el-skeleton
      v-else-if="loading"
      :rows="4"
      animated
      data-testid="content-module-where-used-loading"
    />

    <template v-else-if="rows.length > 0">
      <AppDataTable :data="rows" data-testid="content-module-where-used-table">
        <el-table-column
          :label="t('contentModules.detail.whereUsed.columns.name')"
          min-width="220"
        >
          <template #default="{ row }">
            <EntityLinkCell
              :label="row.name"
              :subtitle="row.externalId"
              :to="templateDetailLink(row.id)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="groupCode"
          :label="t('contentModules.detail.whereUsed.columns.group')"
          width="140"
        />
        <el-table-column
          prop="lifecycleStatus"
          :label="t('contentModules.detail.whereUsed.columns.status')"
          width="160"
        />
        <el-table-column
          :label="t('contentModules.detail.whereUsed.columns.pinnedVersion')"
          width="160"
        >
          <template #default="{ row }">
            {{
              row.pinnedSemanticVersion?.trim()
                ? row.pinnedSemanticVersion
                : t('contentModules.detail.whereUsed.pinnedVersionUnset')
            }}
          </template>
        </el-table-column>
      </AppDataTable>
      <AppTablePagination
        v-model:current-page="currentPage"
        :page-size="SERVER_TABLE_PAGE_SIZE"
        :total="totalElements"
      />
    </template>

    <EmptyStatePanel
      v-else
      title-key="contentModules.detail.whereUsed.empty"
      description-key="contentModules.detail.whereUsed.emptyDescription"
      data-testid="content-module-where-used-empty"
    />
  </section>
</template>

<style scoped lang="scss">
.where-used-hint {
  margin: 0 0 var(--space-3);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}
</style>
