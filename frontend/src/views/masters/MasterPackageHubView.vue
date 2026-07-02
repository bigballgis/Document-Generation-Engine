<script setup lang="ts">

import { computed, onMounted, onUnmounted, ref } from 'vue'

import { useI18n } from 'vue-i18n'

import { useRoute, useRouter } from 'vue-router'

import MasterImpactPanel from '@/components/masters/MasterImpactPanel.vue'

import MasterMetadataEditDialog from '@/components/masters/MasterMetadataEditDialog.vue'

import MasterReplaceFileDialog from '@/components/masters/MasterReplaceFileDialog.vue'

import MasterSubmitReviewDialog from '@/components/masters/MasterSubmitReviewDialog.vue'

import MasterRevisionLinesPanel from '@/components/masters/MasterRevisionLinesPanel.vue'

import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'

import MasterWorkflowBanner from '@/components/masters/MasterWorkflowBanner.vue'

import MasterDesignerJourneyBlock from '@/components/journey/MasterDesignerJourneyBlock.vue'

import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'

import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'

import AppPageLayout from '@/components/layout/AppPageLayout.vue'

import PageHeader from '@/components/layout/PageHeader.vue'

import { ROUTE_PATH_BY_KEY, ROUTE_KEYS, masterRevisionDetailPath } from '@/routing/routeKeys'

import { useMastersStore } from '@/stores/masters'

import { useSessionStore } from '@/stores/session'

import { useCapabilities } from '@/composables/useCapabilities'

import { shouldShowMasterDesignerJourney } from '@/utils/masterDesignerJourney'

import { ElMessage } from 'element-plus'

import type { ComponentPublicInstance } from 'vue'



const { t, te } = useI18n()

const route = useRoute()

const router = useRouter()

const mastersStore = useMastersStore()

const sessionStore = useSessionStore()

const { manageMasters, reviewMasters } = useCapabilities()



const metadataEditOpen = ref(false)

const replaceFileOpen = ref(false)

const submitReviewOpen = ref(false)

const loadFailed = ref(false)

const downloading = ref(false)

const currentRevisionLineId = ref<string | undefined>(undefined)

const revisionLinesPanelRef = ref<ComponentPublicInstance<{ reload: () => Promise<void> }> | null>(

  null,

)



const masterId = computed(() => String(route.params.masterId ?? ''))

const master = computed(() => mastersStore.selectedMaster)



const canEditMetadata = computed(() => {

  if (!manageMasters.value || !master.value) {

    return false

  }

  return (

    master.value.status === 'DRAFT' ||

    master.value.status === 'REJECTED' ||

    master.value.status === 'APPROVED'

  )

})

const canReplaceFile = computed(() => {

  if (!manageMasters.value || !master.value) {

    return false

  }

  return master.value.status !== 'PENDING_REVIEW'

})

const showDesignerJourney = computed(() => {

  if (!master.value) {

    return false

  }

  return shouldShowMasterDesignerJourney({

    roles: sessionStore.session?.roles ?? [],

    manageMasters: manageMasters.value,

    reviewMasters: reviewMasters.value,

    status: master.value.status,

  })

})

const journeyContext = computed(() => {

  if (!master.value) {

    return null

  }

  return {

    status: master.value.status,

    originalFilename: master.value.originalFilename,

    anchorCount: master.value.anchors.length,

    reviewHistory: master.value.reviewHistory,

  }

})

const canWriteJourney = computed(

  () => Boolean(manageMasters.value && master.value && master.value.status !== 'PENDING_REVIEW'),

)

const errorMessage = computed(() => {

  const key = mastersStore.lastErrorMessageKey

  if (!key) {

    return ''

  }

  return te(key) ? t(key) : t('masters.error.loadDetail')

})



onMounted(async () => {

  await reloadMaster()

})



async function reloadMaster() {

  loadFailed.value = false

  try {

    await mastersStore.fetchMaster(masterId.value)

    await mastersStore.fetchImpactAnalysis(masterId.value)

    await revisionLinesPanelRef.value?.reload()

    const page = await mastersStore.fetchRevisionLines(masterId.value, 0, 5)

    const currentLine = page.content.find((line) => line.current) ?? page.content[0]

    currentRevisionLineId.value = currentLine?.id

  } catch {

    loadFailed.value = true

  }

}



onUnmounted(() => {

  mastersStore.clearSelected()

})



function goBack() {

  router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.masterManagement])

}



async function handleDownloadCurrent() {

  downloading.value = true

  try {

    await mastersStore.downloadMasterFile(masterId.value)

    ElMessage.success(t('masters.download.success'))

  } catch {

    ElMessage.error(errorMessage.value || t('masters.error.download'))

  } finally {

    downloading.value = false

  }

}



async function handleMetadataUpdate(payload: { name: string; description: string | null }) {

  try {

    await mastersStore.updateMasterMetadata(masterId.value, payload)

    metadataEditOpen.value = false

    ElMessage.success(t('masters.metadata.success'))

  } catch {

    ElMessage.error(errorMessage.value || t('masters.error.updateMetadata'))

  }

}



async function handleReplaceFile(file: File) {

  try {

    await mastersStore.replaceMasterFile(masterId.value, file)

    replaceFileOpen.value = false

    await mastersStore.fetchImpactAnalysis(masterId.value)

    await revisionLinesPanelRef.value?.reload()

    ElMessage.success(t('masters.replaceFile.success'))

    const page = await mastersStore.fetchRevisionLines(masterId.value, 0, 1)

    const currentLine = page.content.find((line) => line.current) ?? page.content[0]

    if (currentLine) {

      router.push(masterRevisionDetailPath(masterId.value, currentLine.id))

    }

  } catch {

    ElMessage.error(errorMessage.value || t('masters.error.replaceFile'))

  }

}



async function handleSubmitReview(payload: { changeSummary: string }) {

  try {

    await mastersStore.submitReview(masterId.value, payload)

    submitReviewOpen.value = false

    ElMessage.success(t('masters.submitReview.success'))

    await reloadMaster()

  } catch {

    ElMessage.error(errorMessage.value || t('masters.error.submitReview'))

  }

}



</script>



<template>

  <AppPageLayout>

    <PageHeader

      show-back

      :back-label="t('masters.hub.backToList')"

      :title="master?.name ?? t('masters.hub.loadingTitle')"

      :description="master ? t('masters.hub.groupLabel', { groupCode: master.groupCode }) : undefined"

      @back="goBack"

    >

      <template v-if="master" #actions>

        <MasterStatusBadge :status="master.status" />

        <el-button :loading="downloading" @click="handleDownloadCurrent">

          {{ t('masters.download.action') }}

        </el-button>

        <el-button v-if="canReplaceFile" @click="replaceFileOpen = true">

          {{ t('masters.replaceFile.open') }}

        </el-button>

        <el-button v-if="canEditMetadata" @click="metadataEditOpen = true">

          {{ t('masters.metadata.edit') }}

        </el-button>

      </template>

    </PageHeader>

    <p v-if="master?.description" class="header-extra">{{ master.description }}</p>



    <LoadErrorPanel

      v-if="loadFailed"

      :message-key="mastersStore.lastErrorMessageKey ?? 'masters.error.loadDetail'"

      @retry="reloadMaster"

    />



    <el-skeleton v-else-if="mastersStore.loadingDetail" :rows="8" animated />



    <EmptyStatePanel

      v-else-if="!master"

      title-key="masters.hub.notFoundTitle"

      description-key="masters.hub.notFoundDescription"

    />



    <template v-else-if="master">

      <MasterDesignerJourneyBlock
        v-if="showDesignerJourney && journeyContext"
        :journey-context="journeyContext"
        :master-id="masterId"
        :current-revision-line-id="currentRevisionLineId"
        :can-write="canWriteJourney"
        :show-primary-cta="false"
        :enable-workspace-link="false"
      />



      <MasterWorkflowBanner :master="master" />



      <MasterRevisionLinesPanel ref="revisionLinesPanelRef" :master-id="masterId" />



      <MasterImpactPanel :impact="mastersStore.impactAnalysis" />

    </template>



    <MasterMetadataEditDialog

      v-if="master"

      v-model="metadataEditOpen"

      :initial-name="master.name"

      :initial-description="master.description"

      :loading="mastersStore.submitting"

      @submit="handleMetadataUpdate"

    />

    <MasterReplaceFileDialog

      v-if="master"

      v-model="replaceFileOpen"

      :current-filename="master.originalFilename"

      :loading="mastersStore.submitting"

      @submit="handleReplaceFile"

    />

    <MasterSubmitReviewDialog v-model="submitReviewOpen" @submit="handleSubmitReview" />

  </AppPageLayout>

</template>



<style scoped lang="scss">

.header-extra {

  margin: calc(-1 * var(--space-4)) 0 var(--space-6);

  color: var(--text-secondary);

}

</style>

