<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getMaster } from '@/api/masters'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import type { TemplateDetail } from '@/types/template'

const props = defineProps<{
  template: TemplateDetail
  formatDateTime: (value: string) => string
}>()

const { t } = useI18n()
const { masterDetailLink } = useEntityLinkTargets()
const masterName = ref<string | null>(null)

onMounted(() => {
  void loadMasterName()
})

async function loadMasterName() {
  try {
    const master = await getMaster(props.template.masterId)
    masterName.value = master.name
  } catch {
    masterName.value = null
  }
}
</script>

<template>
  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.detail.summaryTitle') }}</h2>
    <dl class="summary-grid">
      <div>
        <dt>{{ t('templates.detail.externalId') }}</dt>
        <dd>{{ template.externalId }}</dd>
      </div>
      <div>
        <dt>{{ t('templates.detail.masterId') }}</dt>
        <dd>
          <EntityLinkCell
            :label="masterName ?? template.masterId"
            :to="masterDetailLink(template.masterId)"
          />
        </dd>
      </div>
      <div>
        <dt>{{ t('templates.detail.releaseVersion') }}</dt>
        <dd>{{ template.releaseVersion ?? t('templates.detail.noReleaseVersion') }}</dd>
      </div>
      <div>
        <dt>{{ t('templates.detail.updatedAt') }}</dt>
        <dd>{{ formatDateTime(template.updatedAt) }}</dd>
      </div>
    </dl>
    <p class="description">
      {{ template.description ?? t('templates.detail.noDescription') }}
    </p>
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.125rem;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin: 0 0 1rem;

  dt {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0.25rem 0 0;
    font-weight: 500;
  }
}

.description {
  margin: 0;
  color: var(--text-muted);
}
</style>
