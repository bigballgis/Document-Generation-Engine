import { nextTick, onMounted, ref, watch, type Ref } from 'vue'
import type { ApiPolicyDomain } from '@/types/apiPolicyDomain'

const ADVANCED_POLICY_DOMAINS: ApiPolicyDomain[] = [
  'OUTPUT_POLICY',
  'BATCH_LIMIT',
  'ENCRYPTION_CAPABILITY',
]

export function useApiPolicyDomainAnchor(
  initialDomainAnchor: Ref<ApiPolicyDomain | null | undefined>,
) {
  const advancedExpanded = ref<string[]>([])

  function applyDomainAnchor(domain: ApiPolicyDomain) {
    if (ADVANCED_POLICY_DOMAINS.includes(domain)) {
      advancedExpanded.value = ['advanced']
    }
    void nextTick(() => {
      document
        .getElementById(`policy-domain-${domain}`)
        ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }

  onMounted(() => {
    if (initialDomainAnchor.value) {
      applyDomainAnchor(initialDomainAnchor.value)
    }
  })

  watch(initialDomainAnchor, (domain) => {
    if (domain) {
      applyDomainAnchor(domain)
    }
  })

  return { advancedExpanded }
}
