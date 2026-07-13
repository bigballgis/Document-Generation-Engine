import * as apiPolicyApi from '@/api/apiPolicy'
import { resolveApiErrorMessageKey } from '@/api/http'
import type {
  ApiCredentialCreated,
  ApiPolicy,
  ApiPolicyImpactPreview,
  UpsertApiPolicyPayload,
} from '@/types/template'
import type { ApiPolicyDomain, ApiPolicyDomainFormMap, InvocationRetentionDomainForm } from '@/types/apiPolicyDomain'
import type { ApiPolicyEntry } from '@/stores/apiPolicyStoreTypes'

export function createApiPolicyMutationActions(deps: {
  entryFor: (templateId: string) => ApiPolicyEntry
  fetchCredentials: (templateId: string) => Promise<void>
}) {
  const { entryFor, fetchCredentials } = deps

  async function savePolicy(
    templateId: string,
    payload: UpsertApiPolicyPayload,
  ): Promise<ApiPolicy> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      entry.policy = await apiPolicyApi.upsertApiPolicy(templateId, payload)
      return entry.policy
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.savePolicy')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function previewImpact(
    templateId: string,
    payload: UpsertApiPolicyPayload,
  ): Promise<ApiPolicyImpactPreview> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      return await apiPolicyApi.fetchApiPolicyImpactPreview(templateId, payload)
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.previewPolicyImpact')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function savePolicyDomain<D extends ApiPolicyDomain>(
    templateId: string,
    domain: D,
    payload: ApiPolicyDomainFormMap[D],
    confirmed = true,
  ): Promise<ApiPolicy> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      entry.policy = await apiPolicyApi.saveApiPolicyDomain(templateId, domain, payload, confirmed)
      return entry.policy
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.savePolicy')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function saveInvocationRetentionDomain(
    templateId: string,
    payload: InvocationRetentionDomainForm,
    confirmed = true,
  ): Promise<ApiPolicy> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      entry.policy = await apiPolicyApi.saveInvocationRetentionDomain(templateId, payload, confirmed)
      return entry.policy
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.savePolicy')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function createCredential(templateId: string): Promise<ApiCredentialCreated> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      entry.lastCreatedCredential = await apiPolicyApi.createCredential(templateId)
      entry.lastRotatedCredential = null
      await fetchCredentials(templateId)
      return entry.lastCreatedCredential
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.createCredential')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function rotateCredential(templateId: string, credentialId: string) {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      const rotated = await apiPolicyApi.rotateCredential(templateId, credentialId)
      entry.lastRotatedCredential = {
        credentialId: rotated.credentialId,
        externalId: rotated.externalId,
        secret: rotated.secret,
      }
      entry.lastCreatedCredential = null
      await fetchCredentials(templateId)
      return rotated
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.rotateCredential')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function revokeCredential(templateId: string, credentialId: string) {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      await apiPolicyApi.revokeCredential(templateId, credentialId)
      await fetchCredentials(templateId)
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.revokeCredential')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  return {
    savePolicy,
    previewImpact,
    savePolicyDomain,
    saveInvocationRetentionDomain,
    createCredential,
    rotateCredential,
    revokeCredential,
  }
}
