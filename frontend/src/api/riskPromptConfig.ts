import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { RiskPromptConfig, UpsertGlobalRiskPromptConfigPayload } from '@/types/template'

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

export async function getGlobalRiskPromptConfig(): Promise<RiskPromptConfig> {
  const response = await http.get<ApiEnvelope<RiskPromptConfig>>('/risk-prompt-config')
  return unwrap(response.data)
}

export async function upsertGlobalRiskPromptConfig(
  payload: UpsertGlobalRiskPromptConfigPayload,
): Promise<RiskPromptConfig> {
  const response = await http.put<ApiEnvelope<RiskPromptConfig>>('/risk-prompt-config', payload)
  return unwrap(response.data)
}
