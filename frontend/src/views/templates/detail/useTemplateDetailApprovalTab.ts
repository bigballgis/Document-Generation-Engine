import { computed, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  listInvalidBindings,
  mapBindingGateIssueItems,
  type BindingGateIssueItem,
} from '@/utils/templateBindingGateDisplay'
import type { BindingValidationResult } from '@/types/template'
import {
  buildDevWorkspaceQuery,
  resolveApprovalSubTabFromQuery,
} from '@/views/templates/templateDevWorkspaceTabs'
import { type TemplateApprovalSubTab } from '@/views/templates/templateApprovalSubTabs'

export interface UseTemplateDetailApprovalTabOptions {
  bindingGateResult: Ref<BindingValidationResult | null>
}

export function useTemplateDetailApprovalTab(options: UseTemplateDetailApprovalTabOptions) {
  const { t, te } = useI18n()
  const route = useRoute()
  const router = useRouter()

  const activeSubTab = ref<TemplateApprovalSubTab>(resolveApprovalSubTabFromQuery(route.query))

  watch(
    () => [route.query.approvalTab, route.query.focus],
    () => {
      activeSubTab.value = resolveApprovalSubTabFromQuery(route.query)
    },
  )

  watch(activeSubTab, (tab) => {
    if (resolveApprovalSubTabFromQuery(route.query) === tab) {
      return
    }
    void router.replace({
      query: buildDevWorkspaceQuery(route.query, 'approval', tab),
    })
  })

  const bindingGateIssues = computed(() =>
    options.bindingGateResult.value
      ? mapBindingGateIssueItems(options.bindingGateResult.value.summary)
      : [],
  )

  const bindingGateIssueMessageKey: Record<BindingGateIssueItem['issueKey'], string> = {
    missingAnchor: 'templates.bindingGate.issueMissingAnchor',
    duplicateBinding: 'templates.bindingGate.issueDuplicateBinding',
    incompatibleContentType: 'templates.bindingGate.issueIncompatibleContentType',
  }

  const invalidBindings = computed(() =>
    options.bindingGateResult.value
      ? listInvalidBindings(options.bindingGateResult.value.bindings)
      : [],
  )

  function resolveBindingStatusLabel(status: string | undefined): string {
    if (!status) {
      return status ?? ''
    }
    const key = `templates.bindingGate.status.${status}`
    return te(key) ? t(key) : status
  }

  return {
    t,
    activeSubTab,
    bindingGateIssues,
    bindingGateIssueMessageKey,
    invalidBindings,
    resolveBindingStatusLabel,
  }
}
