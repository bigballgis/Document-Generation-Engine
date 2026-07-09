import type { ComputedRef, InjectionKey, Ref } from 'vue'
import type { ApiPolicyDomainEditorForms } from '@/composables/useApiPolicyDomainEditorActions'
import type { ApiPolicyDomain } from '@/types/apiPolicyDomain'
import type { ApiPolicy } from '@/types/template'

export interface ApiPolicyDomainEditorContext {
  apiPolicy: Ref<ApiPolicy | null>
  forms: ApiPolicyDomainEditorForms
  allowedAdGroupsText: ComputedRef<string>
  currentSummary: (domain: ApiPolicyDomain, t: (key: string) => string) => string
}

export const API_POLICY_DOMAIN_EDITOR_KEY: InjectionKey<ApiPolicyDomainEditorContext> = Symbol(
  'apiPolicyDomainEditor',
)
