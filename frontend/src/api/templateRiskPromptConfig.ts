import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  DecisionFormConfig,
  TemplateRiskPromptConfig,
  UpsertTemplateRiskPromptConfigPayload,
} from '@/types/template'

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

export async function getTemplateRiskPromptConfig(
  templateId: string,
): Promise<TemplateRiskPromptConfig> {
  const response = await http.get<ApiEnvelope<TemplateRiskPromptConfig>>(
    `/templates/${templateId}/risk-prompt-config`,
  )
  return unwrap(response.data)
}

export async function upsertTemplateRiskPromptConfig(
  templateId: string,
  payload: UpsertTemplateRiskPromptConfigPayload,
): Promise<TemplateRiskPromptConfig> {
  const response = await http.put<ApiEnvelope<TemplateRiskPromptConfig>>(
    `/templates/${templateId}/risk-prompt-config`,
    payload,
  )
  return unwrap(response.data)
}

export async function getDecisionFormConfig(templateId: string): Promise<DecisionFormConfig> {
  const response = await http.get<ApiEnvelope<DecisionFormConfig>>(
    `/templates/${templateId}/lifecycle/decision-form-config`,
  )
  return unwrap(response.data)
}
