import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { DeleteTemplatePayload, LifecycleGovernanceAction } from '@/types/template'
import type { ComputedRef } from 'vue'

type GovernanceAction = 'stop' | 'restore' | 'deprecate'

export interface UseTemplateLifecycleGovernanceOptions {
  templateId: ComputedRef<string>
  errorMessage: ComputedRef<string>
}

const governanceActionConfig = {
  stop: {
    previewAction: 'STOP' as LifecycleGovernanceAction,
    titleKey: 'templates.lifecycle.stopTitle',
    reasonKey: 'templates.lifecycle.stopReasonPrompt',
    confirmTitleKey: 'templates.lifecycle.confirmStopTitle',
    confirmMessageKey: 'templates.lifecycle.confirmStopMessage',
    successKey: 'templates.lifecycle.stopSuccess',
  },
  restore: {
    previewAction: 'RESTORE' as LifecycleGovernanceAction,
    titleKey: 'templates.lifecycle.restoreTitle',
    reasonKey: 'templates.lifecycle.restoreReasonPrompt',
    confirmTitleKey: 'templates.lifecycle.confirmRestoreTitle',
    confirmMessageKey: 'templates.lifecycle.confirmRestoreMessage',
    successKey: 'templates.lifecycle.restoreSuccess',
  },
  deprecate: {
    previewAction: 'DEPRECATE' as LifecycleGovernanceAction,
    titleKey: 'templates.lifecycle.deprecateTitle',
    reasonKey: 'templates.lifecycle.deprecateReasonPrompt',
    confirmTitleKey: 'templates.lifecycle.confirmDeprecateTitle',
    confirmMessageKey: 'templates.lifecycle.confirmDeprecateMessage',
    successKey: 'templates.lifecycle.deprecateSuccess',
  },
} as const

export function useTemplateLifecycleGovernance(options: UseTemplateLifecycleGovernanceOptions) {
  const { templateId, errorMessage } = options

  const { t, te } = useI18n()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const { confirmAction } = useConfirmAction()

  async function buildImpactPreviewMessage(
    action: LifecycleGovernanceAction,
    releaseVersion?: string,
  ): Promise<string> {
    const preview = await templatesStore.fetchLifecycleImpactPreview(templateId.value, {
      action,
      releaseVersion,
    })
    const summary = te(preview.summaryMessageKey)
      ? t(preview.summaryMessageKey)
      : t(`templates.governance.impactSummary.${action}`)
    const callable = preview.callableReleaseVersions.length
      ? t('templates.governance.impactCallableVersions', {
          versions: preview.callableReleaseVersions.join(', '),
        })
      : t('templates.governance.impactNoCallableVersions')
    const defaultRoute = preview.defaultRouteReleaseVersion
      ? t('templates.governance.impactDefaultRoute', {
          version: preview.defaultRouteReleaseVersion,
        })
      : ''
    const routeImpact = preview.defaultRouteImpacted
      ? t('templates.governance.impactDefaultRouteAffected')
      : ''
    return [summary, callable, defaultRoute, routeImpact, t('templates.governance.impactConfirmPrompt')]
      .filter(Boolean)
      .join('\n\n')
  }

  async function handleGovernanceAction(action: GovernanceAction) {
    const config = governanceActionConfig[action]
    let reason = ''
    try {
      const result = await ElMessageBox.prompt(t(config.reasonKey), t(config.titleKey), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        inputValidator: (value) =>
          value.trim().length > 0 ? true : t('templates.lifecycle.reasonRequired'),
      })
      reason = result.value.trim()
    } catch {
      return
    }

    try {
      const impactMessage = await buildImpactPreviewMessage(config.previewAction)
      const confirmBody = [impactMessage, t(config.confirmMessageKey)].join('\n\n')
      await ElMessageBox.confirm(confirmBody, t(config.confirmTitleKey), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      })
    } catch {
      return
    }

    const payload = { reason, confirmed: true }
    try {
      if (action === 'stop') {
        await templatesStore.stopTemplate(templateId.value, payload)
      } else if (action === 'restore') {
        await templatesStore.restoreTemplate(templateId.value, payload)
      } else {
        await templatesStore.deprecateTemplate(templateId.value, payload)
      }
      ElMessage.success(t(config.successKey))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  async function handleDeleteTemplate() {
    let reason = ''
    try {
      const result = await ElMessageBox.prompt(
        t('templates.deleteAction.reasonPrompt'),
        t('templates.deleteAction.title'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          inputValidator: (value) =>
            value.trim().length > 0 ? true : t('templates.deleteAction.reasonRequired'),
        },
      )
      reason = result.value.trim()
    } catch {
      return
    }

    const confirmed = await confirmAction({
      titleKey: 'templates.deleteAction.confirmTitle',
      messageKey: 'templates.deleteAction.confirmMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }

    try {
      const payload: DeleteTemplatePayload = { reason }
      await templatesStore.deleteTemplate(templateId.value, payload)
      ElMessage.success(t('templates.deleteAction.success'))
      router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.delete'))
    }
  }

  return {
    handleGovernanceAction,
    handleDeleteTemplate,
  }
}
