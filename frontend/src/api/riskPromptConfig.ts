import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { RiskPromptConfig, UpsertRiskPromptConfigPayload } from '@/types/template'

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

export async function getRiskPromptConfig(groupCode?: string): Promise<RiskPromptConfig> {
  const response = await http.get<ApiEnvelope<RiskPromptConfig>>('/risk-prompt-config', {
    params: groupCode ? { groupCode } : undefined,
  })
  return unwrap(response.data)
}

export async function upsertRiskPromptConfig(
  payload: UpsertRiskPromptConfigPayload,
): Promise<RiskPromptConfig> {
  const response = await http.put<ApiEnvelope<RiskPromptConfig>>('/risk-prompt-config', payload)
  return unwrap(response.data)
}
