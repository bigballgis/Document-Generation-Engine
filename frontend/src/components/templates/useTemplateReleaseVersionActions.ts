import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { useTemplatesStore } from '@/stores/templates'
import type { LifecycleGovernanceAction } from '@/types/template'
import type { ComputedRef, Ref } from 'vue'

export interface UseTemplateReleaseVersionActionsOptions {
  templateId: Ref<string> | (() => string)
  errorMessage: ComputedRef<string>
  loadVersions: () => Promise<void>
  onChanged: () => void
}

function readValue(source: Ref<string> | (() => string)): string {
  return typeof source === 'function' ? source() : source.value
}

export function useTemplateReleaseVersionActions(
  options: UseTemplateReleaseVersionActionsOptions,
) {
  const { t, te } = useI18n()
  const templatesStore = useTemplatesStore()
  const panelDataStore = useTemplatePanelDataStore()

  async function buildImpactPreviewMessage(
    action: LifecycleGovernanceAction,
    releaseVersion: string,
  ): Promise<string> {
    const preview = await templatesStore.fetchLifecycleImpactPreview(readValue(options.templateId), {
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

  async function handleVersionAction(
    releaseVersion: string,
    action: 'deactivate' | 'restore',
  ) {
    const previewAction: LifecycleGovernanceAction =
      action === 'deactivate' ? 'DEACTIVATE_VERSION' : 'RESTORE_VERSION'
    const reasonKey =
      action === 'deactivate'
        ? 'templates.versions.deactivateReasonPrompt'
        : 'templates.versions.restoreReasonPrompt'
    const titleKey =
      action === 'deactivate'
        ? 'templates.versions.deactivateTitle'
        : 'templates.versions.restoreTitle'
    const confirmTitleKey =
      action === 'deactivate'
        ? 'templates.versions.confirmDeactivateTitle'
        : 'templates.versions.confirmRestoreTitle'
    const confirmMessageKey =
      action === 'deactivate'
        ? 'templates.versions.confirmDeactivateMessage'
        : 'templates.versions.confirmRestoreMessage'
    const successKey =
      action === 'deactivate'
        ? 'templates.versions.deactivateSuccess'
        : 'templates.versions.restoreSuccess'

    let reason = ''
    try {
      const result = await ElMessageBox.prompt(t(reasonKey), t(titleKey), {
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
      const impactMessage = await buildImpactPreviewMessage(previewAction, releaseVersion)
      const confirmBody = [impactMessage, t(confirmMessageKey)].join('\n\n')
      await ElMessageBox.confirm(confirmBody, t(confirmTitleKey), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      })
    } catch {
      return
    }

    const payload = { reason, confirmed: true }
    try {
      const templateId = readValue(options.templateId)
      if (action === 'deactivate') {
        await templatesStore.deactivateTemplateVersion(templateId, releaseVersion, payload)
      } else {
        await templatesStore.restoreTemplateVersion(templateId, releaseVersion, payload)
      }
      ElMessage.success(t(successKey))
      panelDataStore.invalidateVersionLineDomains(templateId)
      await options.loadVersions()
      options.onChanged()
    } catch {
      ElMessage.error(options.errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  return { handleVersionAction }
}
