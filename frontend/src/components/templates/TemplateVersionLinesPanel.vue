<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import * as templatesApi from '@/api/templates'
import {
  templateDevVersionPath,
  templateReleaseDetailPath,
} from '@/routing/routeKeys'
import type { TemplateVersionLineSummary } from '@/types/template'
import { isInFlightVersionLine, versionLineDisplayLabel } from '@/utils/templateVersionLine'

const props = defineProps<{
  templateId: string
  canClone?: boolean
}>()

const emit = defineEmits<{
  cloned: []
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const router = useRouter()

const loading = ref(false)
const loadError = ref(false)
const cloningReleaseVersion = ref<string | null>(null)
const currentPage = ref(1)
const totalElements = ref(0)
const totalPages = ref(0)
const versionLines = ref<TemplateVersionLineSummary[]>([])

const pageSize = SERVER_TABLE_PAGE_SIZE

function lineLabel(row: TemplateVersionLineSummary): string {
  return versionLineDisplayLabel(t, row)
}

async function loadVersionLines() {
  loading.value = true
  loadError.value = false
  try {
    const page = await templatesApi.listTemplateVersionLines(
      props.templateId,
      currentPage.value - 1,
      pageSize,
    )
    versionLines.value = page.content
    totalElements.value = page.totalElements
    totalPages.value = page.totalPages
  } catch {
    loadError.value = true
    versionLines.value = []
    totalElements.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadVersionLines()
})

watch(
  () => props.templateId,
  () => {
    currentPage.value = 1
    void loadVersionLines()
  },
)

watch(currentPage, () => {
  void loadVersionLines()
})

function openVersionLine(row: TemplateVersionLineSummary) {
  if (isInFlightVersionLine(row)) {
    router.push(templateDevVersionPath(props.templateId, row.devVersionId))
    return
  }
  if (row.releaseVersion) {
    router.push(templateReleaseDetailPath(props.templateId, row.releaseVersion))
  }
}

const { onRowClick } = useActivatableTableRow<TemplateVersionLineSummary>(openVersionLine)

const showPagination = computed(() => totalPages.value > 1)

function canCloneRow(row: TemplateVersionLineSummary): boolean {
  return Boolean(
    props.canClone &&
      row.releaseVersion &&
      !isInFlightVersionLine(row) &&
      row.cloneable !== false,
  )
}

async function handleClone(row: TemplateVersionLineSummary) {
  if (!row.releaseVersion) {
    return
  }
  cloningReleaseVersion.value = row.releaseVersion
  try {
    const created = await templatesApi.cloneReleaseVersion(props.templateId, row.releaseVersion)
    ElMessage.success(t('templates.versionLines.cloneSuccess'))
    emit('cloned')
    router.push(templateDevVersionPath(props.templateId, created.devVersionId))
  } catch {
    ElMessage.error(t('templates.versionLines.cloneError'))
  } finally {
    cloningReleaseVersion.value = null
  }
}

defineExpose({
  reload: loadVersionLines,
})
</script>

<template>
  <el-card shadow="never" class="version-lines-card">
    <template #header>
      <div class="card-header">
        <span>{{ t('templates.versionLines.title') }}</span>
        <p class="card-hint">{{ t('templates.versionLines.hint') }}</p>
      </div>
    </template>

    <LoadErrorPanel
      v-if="loadError"
      message-key="templates.versionLines.loadError"
      @retry="loadVersionLines"
    />

    <el-skeleton v-else-if="loading" :rows="4" animated />

    <template v-else>
      <AppDataTable
        activatable
        :data="versionLines"
        @row-click="onRowClick"
      >
        <template #empty>
          <el-empty :description="t('templates.versionLines.empty')" />
        </template>
        <el-table-column min-width="200" :label="t('templates.versionLines.line')">
          <template #default="{ row }">
            <span>{{ lineLabel(row) }}</span>
            <el-tag
              v-if="isInFlightVersionLine(row)"
              size="small"
              type="success"
              class="line-tag"
            >
              {{ t('templates.versionLines.inFlightBadge') }}
            </el-tag>
            <el-tag v-else size="small" type="info" class="line-tag">
              {{ t('templates.versionLines.publishedBadge') }}
            </el-tag>
            <el-tag
              v-if="row.defaultRouteTarget"
              size="small"
              type="warning"
              class="line-tag"
            >
              {{ t('templates.versionLines.defaultRouteBadge') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column min-width="140" :label="t('templates.versionLines.status')">
          <template #default="{ row }">
            <TemplateStatusBadge
              :status="row.lifecycleStatus"
              :approval-sub-state="row.approvalSubState"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="devVersionNumber"
          width="120"
          :label="t('templates.versionLines.devVersionNumber')"
        />
        <el-table-column min-width="170" :label="t('templates.versionLines.updatedAt')">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedBy" min-width="120" :label="t('templates.versionLines.updatedBy')" />
        <el-table-column width="180" :label="t('templates.versionLines.actions')">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openVersionLine(row)">
              {{ t('templates.versionLines.viewDetail') }}
            </el-button>
            <el-button
              v-if="canCloneRow(row)"
              link
              type="primary"
              data-version-line-clone
              :loading="cloningReleaseVersion === row.releaseVersion"
              @click.stop="handleClone(row)"
            >
              {{ t('templates.versionLines.clone') }}
            </el-button>
          </template>
        </el-table-column>
      </AppDataTable>

      <AppTablePagination
        v-if="showPagination"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalElements"
      />
    </template>
  </el-card>
</template>

<style scoped lang="scss">
.version-lines-card {
  margin-bottom: 1rem;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.card-hint {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-muted);
}

.line-tag {
  margin-left: 0.5rem;
}
</style>
