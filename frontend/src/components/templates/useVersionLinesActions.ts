import { type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { useTemplatesStore } from '@/stores/templates'
import { templateDevVersionPath } from '@/routing/routeKeys'
import type { LifecycleGovernanceAction, TemplateVersionLineSummary } from '@/types/template'

export function useVersionLinesActions(options: {
  templateId: () => string
  latestPublishedLine: Ref<TemplateVersionLineSummary | undefined>
  errorMessage: Ref<string>
  cloningReleaseVersion: Ref<string | null>
  abandoningDevVersionId: Ref<string | null>
  loadVersionLines: () => Promise<void>
  emitCloned: () => void
  emitChanged: () => void
}) {
  const { t, te } = useI18n()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const panelDataStore = useTemplatePanelDataStore()

  async function handleClone(row: TemplateVersionLineSummary) {
    if (!row.releaseVersion) {
      return
    }
    options.cloningReleaseVersion.value = row.releaseVersion
    try {
      const created = await panelDataStore.cloneReleaseVersion(
        options.templateId(),
        row.releaseVersion,
      )
      ElMessage.success(t('templates.versionLines.cloneSuccess'))
      options.emitCloned()
      router.push(templateDevVersionPath(options.templateId(), created.devVersionId))
    } catch {
      ElMessage.error(t('templates.versionLines.cloneError'))
    } finally {
      options.cloningReleaseVersion.value = null
    }
  }

  async function handleCreateFromLatestRelease() {
    const row = options.latestPublishedLine.value
    if (!row?.releaseVersion) {
      return
    }
    await handleClone(row)
  }

  async function handleAbandon(row: TemplateVersionLineSummary) {
    try {
      await ElMessageBox.confirm(
        t('templates.versionLines.abandonConfirm'),
        t('templates.versionLines.abandon'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'warning',
        },
      )
    } catch {
      return
    }

    options.abandoningDevVersionId.value = row.devVersionId
    try {
      await panelDataStore.abandonDevVersion(options.templateId(), row.devVersionId)
      ElMessage.success(t('templates.versionLines.abandonSuccess'))
      options.emitChanged()
      panelDataStore.invalidateVersionLineDomains(options.templateId())
      await options.loadVersionLines()
    } catch {
      ElMessage.error(t('templates.versionLines.abandonError'))
    } finally {
      options.abandoningDevVersionId.value = null
    }
  }

  async function buildImpactPreviewMessage(
    action: LifecycleGovernanceAction,
    releaseVersion: string,
  ): Promise<string> {
    const preview = await templatesStore.fetchLifecycleImpactPreview(options.templateId(), {
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
      if (action === 'deactivate') {
        await templatesStore.deactivateTemplateVersion(
          options.templateId(),
          releaseVersion,
          payload,
        )
      } else {
        await templatesStore.restoreTemplateVersion(options.templateId(), releaseVersion, payload)
      }
      ElMessage.success(t(successKey))
      options.emitChanged()
      panelDataStore.invalidateVersionLineDomains(options.templateId())
      await options.loadVersionLines()
    } catch {
      ElMessage.error(options.errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  return {
    handleClone,
    handleCreateFromLatestRelease,
    handleAbandon,
    handleVersionAction,
  }
}
