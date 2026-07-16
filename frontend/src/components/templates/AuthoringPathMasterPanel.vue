<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getMaster } from '@/api/masters'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { sortMasterAnchorsByDocumentSequence } from '@/utils/masterAnchorDocumentOrder'
import type { MasterAnchor, MasterDocumentDetail } from '@/types/master'

const props = defineProps<{
  masterId: string
}>()

const { t } = useI18n()
const { masterDetailLink } = useEntityLinkTargets()

const loading = ref(true)
const loadFailed = ref(false)
const master = ref<MasterDocumentDetail | null>(null)
const anchors = ref<MasterAnchor[]>([])

async function loadMasterSummary() {
  loading.value = true
  loadFailed.value = false
  try {
    const detail = await getMaster(props.masterId)
    master.value = detail
    anchors.value = sortMasterAnchorsByDocumentSequence(detail.anchors ?? [])
  } catch {
    loadFailed.value = true
    master.value = null
    anchors.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadMasterSummary()
})
</script>

<template>
  <section
    class="authoring-path-master"
    data-testid="authoring-path-master-panel"
  >
    <div class="authoring-path-master__heading">
      <h3 class="authoring-path-master__title">{{ t('templates.authoringPathGuide.master.title') }}</h3>
      <p class="authoring-path-master__description">
        {{ t('templates.authoringPathGuide.master.description') }}
      </p>
    </div>

    <el-skeleton v-if="loading" :rows="3" animated />

    <el-alert
      v-else-if="loadFailed"
      type="error"
      show-icon
      :closable="false"
      :title="t('templates.authoringPathGuide.master.loadFailed')"
      data-testid="authoring-path-master-error"
    >
      <el-button type="primary" link data-testid="authoring-path-master-retry" @click="loadMasterSummary">
        {{ t('templates.authoringPathGuide.master.retry') }}
      </el-button>
    </el-alert>

    <template v-else>
      <div class="authoring-path-master__identity" data-testid="authoring-path-master-identity">
        <span class="authoring-path-master__label">{{ t('templates.authoringPathGuide.master.linkedMaster') }}</span>
        <EntityLinkCell
          :label="master?.name ?? masterId"
          :subtitle="master ? t('templates.authoringPathGuide.master.groupLabel', { groupCode: master.groupCode }) : undefined"
          :to="masterDetailLink(masterId)"
        />
      </div>

      <div class="authoring-path-master__anchors" data-testid="authoring-path-master-anchors">
        <p class="authoring-path-master__anchor-count">
          {{ t('templates.authoringPathGuide.master.anchorCount', { count: anchors.length }) }}
        </p>
        <ol v-if="anchors.length > 0" class="authoring-path-master__anchor-list">
          <li
            v-for="anchor in anchors"
            :key="anchor.anchorId"
            class="authoring-path-master__anchor-item"
          >
            <span class="authoring-path-master__anchor-label">
              {{ anchor.displayLabel || anchor.anchorId }}
            </span>
            <span class="authoring-path-master__anchor-id">{{ anchor.anchorId }}</span>
          </li>
        </ol>
        <p v-else class="authoring-path-master__empty">
          {{ t('templates.authoringPathGuide.master.noAnchors') }}
        </p>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss" src="./AuthoringPathMasterPanel.scss"></style>
