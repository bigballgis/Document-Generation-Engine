import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS, masterRevisionDetailPath } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useCapabilities } from '@/composables/useCapabilities'
import { shouldShowMasterDesignerJourney } from '@/utils/masterDesignerJourney'

type HubBodyExpose = {
  reloadRevisionLines: () => Promise<void> | undefined
}

export function useMasterPackageHub() {
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
  const bodyRef = ref<HubBodyExpose | null>(null)

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
      await bodyRef.value?.reloadRevisionLines()
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
      await bodyRef.value?.reloadRevisionLines()
      ElMessage.success(t('masters.replaceFile.success'))
      const page = await mastersStore.fetchRevisionLines(masterId.value, 0, 1)
      const currentLine = page.content.find((line) => line.current) ?? page.content[0]
      if (currentLine) {
        router.push(masterRevisionDetailPath(masterId.value, currentLine.id))
      }
    } catch {
      // Keep dialog open — inline translated error via serverErrorKey (LR-C10-B).
    }
  }

  function clearReplaceServerError() {
    mastersStore.lastErrorMessageKey = null
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

  return {
    t,
    mastersStore,
    metadataEditOpen,
    replaceFileOpen,
    submitReviewOpen,
    loadFailed,
    downloading,
    currentRevisionLineId,
    bodyRef,
    masterId,
    master,
    canEditMetadata,
    canReplaceFile,
    showDesignerJourney,
    journeyContext,
    canWriteJourney,
    reloadMaster,
    goBack,
    handleDownloadCurrent,
    handleMetadataUpdate,
    handleReplaceFile,
    clearReplaceServerError,
    handleSubmitReview,
  }
}
