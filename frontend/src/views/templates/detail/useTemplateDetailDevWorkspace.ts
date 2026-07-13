import { computed, ref, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSubmitTestEligibility } from '@/composables/useSubmitTestEligibility'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import {
  TEMPLATE_DEV_WORKSPACE_TABS,
  buildDevWorkspaceQuery,
  resolveTemplateDevWorkspaceTabFromQuery,
  templateDevWorkspaceTabLabelKey,
  type TemplateDevWorkspaceTab,
} from '@/views/templates/templateDevWorkspaceTabs'

export type UseTemplateDetailDevWorkspaceOptions = {
  templateId: MaybeRefOrGetter<string>
  openSubmitForTestDialog: MaybeRefOrGetter<boolean | undefined>
  onClearOpenSubmitForTestDialog: () => void
  onSubmitForTest: (comment: string) => void
  onBatchCompleted: () => void
}

export function useTemplateDetailDevWorkspace(options: UseTemplateDetailDevWorkspaceOptions) {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const panelDataStore = useTemplatePanelDataStore()

  const activeWorkspaceTab = ref<TemplateDevWorkspaceTab>(
    resolveTemplateDevWorkspaceTabFromQuery(route.query),
  )
  const testDataSetCount = ref(0)
  const submitForTestDialogOpen = ref(false)

  const batchDialogVisible = ref(false)
  const batchDialogRunId = ref('')
  const batchDialogStreamUrl = ref('')
  const batchRunning = ref(false)

  const { isEligible, tooltipContent, refresh: refreshEligibility } = useSubmitTestEligibility(
    toValue(options.templateId),
  )

  const submitTooltipContent = computed(() => {
    if (testDataSetCount.value === 0) {
      return t('templates.testPreview.workflow.noDataSetsTooltip')
    }
    return tooltipContent.value
  })

  const submitTooltipDisabled = computed(() => isEligible.value && testDataSetCount.value > 0)

  const workspaceTabs = computed(() =>
    TEMPLATE_DEV_WORKSPACE_TABS.map((name) => ({
      name,
      labelKey: templateDevWorkspaceTabLabelKey(name),
    })),
  )

  watch(
    () => route.query,
    () => {
      const resolved = resolveTemplateDevWorkspaceTabFromQuery(route.query)
      if (activeWorkspaceTab.value !== resolved) {
        activeWorkspaceTab.value = resolved
      }
    },
    { deep: true },
  )

  watch(activeWorkspaceTab, (tab) => {
    if (resolveTemplateDevWorkspaceTabFromQuery(route.query) === tab) {
      return
    }
    void router.replace({ query: buildDevWorkspaceQuery(route.query, tab) })
  })

  watch(
    () => toValue(options.openSubmitForTestDialog),
    (requested) => {
      if (requested) {
        activeWorkspaceTab.value = 'testing'
        submitForTestDialogOpen.value = true
        options.onClearOpenSubmitForTestDialog()
      }
    },
  )

  function handleSubmitForTestConfirm(comment: string) {
    submitForTestDialogOpen.value = false
    options.onSubmitForTest(comment)
  }

  function requestSubmitForTestDialog() {
    submitForTestDialogOpen.value = true
  }

  async function handleRunFullTest() {
    try {
      await ElMessageBox.confirm(
        t('templates.batchTest.confirmMessage', { count: testDataSetCount.value }),
        t('templates.batchTest.confirmTitle'),
        {
          confirmButtonText: t('templates.batchTest.confirmButton'),
          cancelButtonText: t('templates.batchTest.cancelButton'),
          type: 'info',
        },
      )
    } catch {
      return
    }

    batchRunning.value = true
    try {
      const result = await panelDataStore.runBatchTest(toValue(options.templateId))
      batchDialogRunId.value = result.runId
      batchDialogStreamUrl.value = result.streamUrl
      batchDialogVisible.value = true
    } catch {
      ElMessage.error(t('templates.batchTest.error.start'))
    } finally {
      batchRunning.value = false
    }
  }

  function handleBatchCompleted() {
    options.onBatchCompleted()
    void refreshEligibility()
  }

  watch(batchDialogVisible, (visible, wasVisible) => {
    if (wasVisible && !visible) {
      void refreshEligibility()
    }
  })

  watch(
    activeWorkspaceTab,
    (tab) => {
      if (tab === 'testing') {
        void refreshEligibility()
      }
    },
    { immediate: false },
  )

  return {
    activeWorkspaceTab,
    workspaceTabs,
    testDataSetCount,
    submitForTestDialogOpen,
    batchDialogVisible,
    batchDialogRunId,
    batchDialogStreamUrl,
    batchRunning,
    isEligible,
    submitTooltipContent,
    submitTooltipDisabled,
    handleSubmitForTestConfirm,
    requestSubmitForTestDialog,
    handleRunFullTest,
    handleBatchCompleted,
  }
}
