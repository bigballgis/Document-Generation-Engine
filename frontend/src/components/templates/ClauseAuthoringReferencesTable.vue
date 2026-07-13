<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import type { TemplateContentModuleReference } from '@/types/template'

defineProps<{
  references: TemplateContentModuleReference[]
  loading: boolean
  editable: boolean
  resolveModuleName: (moduleId: string) => string
}>()

const emit = defineEmits<{
  preview: [reference: TemplateContentModuleReference]
  editPin: [reference: TemplateContentModuleReference]
  editClause: [reference: TemplateContentModuleReference]
}>()

const { t } = useI18n()
</script>

<template>
  <AppDataTable v-if="references.length > 0" :data="references" class="references-table">
    <el-table-column
      prop="referenceKey"
      :label="t('templates.clauseAuthoring.columns.referenceKey')"
      min-width="160"
    />
    <el-table-column
      :label="t('templates.clauseAuthoring.columns.moduleName')"
      min-width="220"
    >
      <template #default="{ row }">
        {{ resolveModuleName(row.moduleId) }}
      </template>
    </el-table-column>
    <el-table-column
      prop="semanticVersion"
      :label="t('templates.clauseAuthoring.columns.semanticVersion')"
      width="120"
    />
    <el-table-column
      :label="t('templates.clauseAuthoring.columns.locked')"
      width="100"
    >
      <template #default="{ row }">
        {{
          row.locked
            ? t('templates.clauseAuthoring.lockedYes')
            : t('templates.clauseAuthoring.lockedNo')
        }}
      </template>
    </el-table-column>
    <el-table-column
      :label="t('templates.clauseAuthoring.columns.actions')"
      width="280"
      fixed="right"
    >
      <template #default="{ row }">
        <el-button link type="primary" @click="emit('preview', row)">
          {{ t('templates.clauseAuthoring.preview') }}
        </el-button>
        <el-button
          v-if="editable"
          link
          type="primary"
          :disabled="row.locked"
          @click="emit('editPin', row)"
        >
          {{ t('templates.clauseAuthoring.editPin') }}
        </el-button>
        <el-button
          v-if="editable"
          link
          type="primary"
          @click="emit('editClause', row)"
        >
          {{ t('templates.clauseAuthoring.editClause') }}
        </el-button>
      </template>
    </el-table-column>
  </AppDataTable>

  <EmptyStatePanel
    v-else-if="!loading"
    title-key="templates.clauseAuthoring.empty"
    description-key="templates.clauseAuthoring.emptyDescription"
  />
</template>

<style scoped lang="scss">
.references-table {
  margin-top: 0.5rem;
}
</style>
