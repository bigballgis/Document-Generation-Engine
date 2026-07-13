import type { Ref } from 'vue'
import * as templatesApi from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/http'
import type {
  LifecycleCommentPayload,
  LifecycleDecisionPayload,
  LifecycleGovernancePayload,
  LifecycleImpactPreviewRequest,
  PublishTemplatePayload,
  TemplateDetail,
  TemplateSummary,
} from '@/types/template'
import { applyUpdatedTemplate } from '@/stores/templatesStoreHelpers'

export function createTemplatesLifecycleActions(deps: {
  submitting: Ref<boolean>
  lastErrorMessageKey: Ref<string | null>
  selectedTemplate: Ref<TemplateDetail | null>
  templates: Ref<TemplateSummary[]>
}) {
  const { submitting, lastErrorMessageKey, selectedTemplate, templates } = deps

  async function runLifecycleAction(action: () => Promise<TemplateDetail>) {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await action()
      applyUpdatedTemplate(selectedTemplate, templates, updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.lifecycle')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function submitForTest(templateId: string, payload: LifecycleCommentPayload) {
    return runLifecycleAction(() => templatesApi.submitForTest(templateId, payload))
  }

  async function recordTestDecision(templateId: string, payload: LifecycleDecisionPayload) {
    return runLifecycleAction(() => templatesApi.recordTestDecision(templateId, payload))
  }

  async function submitForApproval(templateId: string, payload: LifecycleCommentPayload) {
    return runLifecycleAction(() => templatesApi.submitForApproval(templateId, payload))
  }

  async function recordApprovalDecision(templateId: string, payload: LifecycleDecisionPayload) {
    return runLifecycleAction(() => templatesApi.recordApprovalDecision(templateId, payload))
  }

  async function publishTemplate(templateId: string, payload: PublishTemplatePayload) {
    return runLifecycleAction(() => templatesApi.publishTemplate(templateId, payload))
  }

  async function stopTemplate(templateId: string, payload: LifecycleGovernancePayload) {
    return runLifecycleAction(() => templatesApi.stopTemplate(templateId, payload))
  }

  async function restoreTemplate(templateId: string, payload: LifecycleGovernancePayload) {
    return runLifecycleAction(() => templatesApi.restoreTemplate(templateId, payload))
  }

  async function deprecateTemplate(templateId: string, payload: LifecycleGovernancePayload) {
    return runLifecycleAction(() => templatesApi.deprecateTemplate(templateId, payload))
  }

  async function fetchLifecycleImpactPreview(
    templateId: string,
    payload: LifecycleImpactPreviewRequest,
  ) {
    lastErrorMessageKey.value = null
    try {
      return await templatesApi.fetchLifecycleImpactPreview(templateId, payload)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.lifecycle')
      throw error
    }
  }

  async function deactivateTemplateVersion(
    templateId: string,
    releaseVersion: string,
    payload: LifecycleGovernancePayload,
  ) {
    return runLifecycleAction(() =>
      templatesApi.deactivateTemplateVersion(templateId, releaseVersion, payload),
    )
  }

  async function restoreTemplateVersion(
    templateId: string,
    releaseVersion: string,
    payload: LifecycleGovernancePayload,
  ) {
    return runLifecycleAction(() =>
      templatesApi.restoreTemplateVersion(templateId, releaseVersion, payload),
    )
  }

  return {
    submitForTest,
    recordTestDecision,
    submitForApproval,
    recordApprovalDecision,
    publishTemplate,
    stopTemplate,
    restoreTemplate,
    deprecateTemplate,
    fetchLifecycleImpactPreview,
    deactivateTemplateVersion,
    restoreTemplateVersion,
  }
}
