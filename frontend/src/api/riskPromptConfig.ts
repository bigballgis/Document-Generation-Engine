import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { RiskPromptConfig, UpsertGlobalRiskPromptConfigPayload } from '@/types/template'

export async function getGlobalRiskPromptConfig(): Promise<RiskPromptConfig> {
  const response = await http.get<ApiEnvelope<RiskPromptConfig>>('/risk-prompt-config')
  return unwrapEnvelope(response.data)
}

export async function upsertGlobalRiskPromptConfig(
  payload: UpsertGlobalRiskPromptConfigPayload,
): Promise<RiskPromptConfig> {
  const response = await http.put<ApiEnvelope<RiskPromptConfig>>('/risk-prompt-config', payload)
  return unwrapEnvelope(response.data)
}
