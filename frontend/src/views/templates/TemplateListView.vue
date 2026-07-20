<script setup lang="ts">
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import TemplateCreateDialog from '@/components/templates/TemplateCreateDialog.vue'
import TemplateImportDialog from '@/components/templates/TemplateImportDialog.vue'
import TemplateListCatalogPanel from '@/views/templates/list/TemplateListCatalogPanel.vue'
import TemplateListWorkflowFilters from '@/views/templates/list/TemplateListWorkflowFilters.vue'
import { useTemplateListCatalog } from '@/views/templates/useTemplateListCatalog'

const {
  t,
  templatesStore,
  authorTemplates,
  exportTemplates,
  templateDetailLink,
  groupCatalogLink,
  activeWorkflowFilter,
  createDialogOpen,
  importDialogOpen,
  currentPage,
  workflowFilterChips,
  catalogTemplates,
  searchQuery,
  filters,
  activeSortKey,
  hasAnyActive,
  activeFilterChips,
  clearAll,
  removeFilterChip,
  catalogToolbarFilters,
  catalogSortOptions,
  showCatalogChrome,
  reloadTemplates,
  clearWorkflowFilter,
  onWorkflowFilterChange,
  handleCreated,
  handleImported,
  activateTemplateRow,
} = useTemplateListCatalog()
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('templates.list.title')"
      :description="t('templates.list.description')"
      :help-text="t('packageCatalog.template.noticeDescription')"
    >
      <template #actions>
        <el-button v-if="exportTemplates" @click="importDialogOpen = true">
          {{ t('templates.import.open') }}
        </el-button>
        <el-button v-if="authorTemplates" type="primary" @click="createDialogOpen = true">
          {{ t('templates.create.open') }}
        </el-button>
      </template>
    </PageHeader>

    <TemplateListWorkflowFilters
      :active-workflow-filter="activeWorkflowFilter"
      :workflow-filter-chips="workflowFilterChips"
      @clear="clearWorkflowFilter"
      @change="onWorkflowFilterChange"
    />

    <LoadErrorPanel
      v-if="templatesStore.lastErrorMessageKey && !templatesStore.loadingList"
      :message-key="templatesStore.lastErrorMessageKey"
      :retryable="templatesStore.lastListErrorRetryable"
      @retry="reloadTemplates"
    />

    <el-skeleton v-else-if="templatesStore.loadingList" :rows="6" animated />

    <template v-else-if="showCatalogChrome">
      <TemplateListCatalogPanel
        v-model:search-query="searchQuery"
        v-model:filter-values="filters"
        v-model:active-sort-key="activeSortKey"
        v-model:current-page="currentPage"
        :catalog-templates="catalogTemplates"
        :catalog-toolbar-filters="catalogToolbarFilters"
        :catalog-sort-options="catalogSortOptions"
        :active-filter-chips="activeFilterChips"
        :has-any-active="hasAnyActive"
        :page-size="templatesStore.templateListSize"
        :total="templatesStore.templateListTotalElements"
        :template-detail-link="templateDetailLink"
        :group-catalog-link="groupCatalogLink"
        @clear="clearAll"
        @remove-chip="removeFilterChip"
        @row-click="activateTemplateRow"
      />
    </template>

    <EmptyStatePanel v-else title-key="templates.list.empty">
      <template v-if="authorTemplates" #actions>
        <el-button type="primary" @click="createDialogOpen = true">
          {{ t('templates.create.open') }}
        </el-button>
      </template>
    </EmptyStatePanel>

    <TemplateCreateDialog v-model="createDialogOpen" @created="handleCreated" />
    <TemplateImportDialog v-model="importDialogOpen" @imported="handleImported" />
  </AppPageLayout>
</template>
