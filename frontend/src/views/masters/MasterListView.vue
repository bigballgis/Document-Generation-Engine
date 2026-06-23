<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import MasterUploadDialog from '@/components/masters/MasterUploadDialog.vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { MASTER_DETAIL_PATH_PREFIX } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import type { MasterDocumentSummary } from '@/types/master'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const router = useRouter()
const mastersStore = useMastersStore()
const sessionStore = useSessionStore()

const uploadDialogOpen = ref(false)
const currentPage = ref(1)
const pageSize = 10

const allMasters = computed(() => mastersStore.masters)
const paginatedMasters = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return allMasters.value.slice(start, start + pageSize)
})
const groupedMasters = computed(() => {
  const grouped = new Map<string, MasterDocumentSummary[]>()
  for (const master of paginatedMasters.value) {
    const existing = grouped.get(master.groupCode) ?? []
    existing.push(master)
    grouped.set(master.groupCode, existing)
  }
  return [...grouped.entries()]
})
const { manageMasters } = useCapabilities()
const canUpload = computed(() => manageMasters.value)
const errorMessage = computed(() => {
  const key = mastersStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('masters.error.loadList')
})

onMounted(async () => {
  try {
    await mastersStore.fetchMasters()
  } catch {
    // Error surfaced via store message key.
  }
})

function openMaster(masterId: string) {
  router.push(`${MASTER_DETAIL_PATH_PREFIX}${masterId}`)
}

async function handleUpload(payload: {
  groupCode: string
  name: string
  description: string
  file: File
}) {
  try {
    const created = await mastersStore.uploadMaster(
      {
        groupCode: payload.groupCode,
        name: payload.name,
        description: payload.description || undefined,
      },
      payload.file,
    )
    uploadDialogOpen.value = false
    ElMessage.success(t('masters.upload.success'))
    router.push(`${MASTER_DETAIL_PATH_PREFIX}${created.id}`)
  } catch {
    ElMessage.error(errorMessage.value || t('masters.error.upload'))
  }
}
</script>

<template>
  <div class="masters-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">{{ sessionStore.session?.displayName }}</p>
        <h1>{{ t('masters.list.title') }}</h1>
        <p>{{ t('masters.list.description') }}</p>
      </div>
      <el-button v-if="canUpload" type="primary" @click="uploadDialogOpen = true">
        {{ t('masters.upload.open') }}
      </el-button>
    </header>

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="mastersStore.loadingList" :rows="6" animated />

    <template v-else-if="groupedMasters.length > 0">
      <section v-for="[groupCode, items] in groupedMasters" :key="groupCode" class="group-section">
        <h2>{{ t('masters.list.groupSection', { groupCode }) }}</h2>
        <el-table
          :data="items"
          stripe
          @row-click="(row: MasterDocumentSummary) => openMaster(row.id)"
        >
          <el-table-column prop="name" :label="t('masters.list.columns.name')" min-width="220" />
          <el-table-column :label="t('masters.list.columns.status')" width="160">
            <template #default="{ row }">
              <MasterStatusBadge :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column
            prop="originalFilename"
            :label="t('masters.list.columns.filename')"
            min-width="180"
          />
          <el-table-column
            prop="anchorCount"
            :label="t('masters.list.columns.anchors')"
            width="100"
          />
          <el-table-column :label="t('masters.list.columns.updatedAt')" min-width="180">
            <template #default="{ row }">
              {{ new Date(row.updatedAt).toLocaleString() }}
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <el-empty v-else :description="t('masters.list.empty')" />

    <el-pagination
      v-if="allMasters.length > pageSize"
      v-model:current-page="currentPage"
      class="list-pagination"
      layout="prev, pager, next"
      :page-size="pageSize"
      :total="allMasters.length"
    />

    <MasterUploadDialog
      v-model="uploadDialogOpen"
      @submit="handleUpload"
    />
  </div>
</template>

<style scoped lang="scss">
.masters-page {
  min-height: 100vh;
  padding: 2rem;
  background: var(--surface-bg);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
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

.eyebrow {
  display: inline-flex;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-weight: 600;
  color: var(--brand-primary);
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

.list-pagination {
  margin-top: 1rem;
  justify-content: flex-end;
}
</style>
