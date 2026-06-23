<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { templateDetailPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateSummary } from '@/types/template'

const { t, te } = useI18n()
const router = useRouter()
const templatesStore = useTemplatesStore()

const testingTemplates = computed(() =>
  templatesStore.templates.filter((item) => item.lifecycleStatus === 'TESTING'),
)

const groupedTestingTemplates = computed(() => {
  const grouped = new Map<string, TemplateSummary[]>()
  for (const template of testingTemplates.value) {
    const existing = grouped.get(template.groupCode) ?? []
    existing.push(template)
    grouped.set(template.groupCode, existing)
  }
  return [...grouped.entries()]
})

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadList')
})

onMounted(async () => {
  try {
    await templatesStore.fetchTemplates()
  } catch {
    // Error surfaced via store message key.
  }
})

function openTemplate(templateId: string) {
  router.push(templateDetailPath(templateId))
}
</script>

<template>
  <div class="workbench-page">
    <header class="page-header">
      <h1>{{ t('workbench.tester.title') }}</h1>
      <p>{{ t('workbench.tester.description') }}</p>
    </header>

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="templatesStore.loadingList" :rows="6" animated />

    <template v-else-if="groupedTestingTemplates.length > 0">
      <section
        v-for="[groupCode, items] in groupedTestingTemplates"
        :key="groupCode"
        class="group-section"
      >
        <h2>{{ t('templates.list.groupSection', { groupCode }) }}</h2>
        <el-table
          :data="items"
          stripe
          @row-click="(row: TemplateSummary) => openTemplate(row.id)"
        >
          <el-table-column prop="name" :label="t('templates.list.columns.name')" min-width="220" />
          <el-table-column prop="externalId" :label="t('templates.list.columns.externalId')" min-width="180" />
          <el-table-column :label="t('templates.list.columns.status')" width="160">
            <template #default="{ row }">
              <TemplateStatusBadge :status="row.lifecycleStatus" />
            </template>
          </el-table-column>
          <el-table-column :label="t('templates.list.columns.updatedAt')" min-width="180">
            <template #default="{ row }">
              {{ new Date(row.updatedAt).toLocaleString() }}
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <el-empty v-else :description="t('workbench.tester.empty')" />
  </div>
</template>

<style scoped lang="scss">
.workbench-page {
  padding: 2rem;
}

.page-header {
  margin-bottom: 1.5rem;

  h1 {
    margin: 0.25rem 0;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.page-alert {
  margin-bottom: 1rem;
}

.group-section {
  margin-bottom: 2rem;

  h2 {
    margin: 0 0 0.75rem;
    font-size: 1.125rem;
  }
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
