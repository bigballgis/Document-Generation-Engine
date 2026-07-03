import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  DecisionFormConfig,
  TemplateRiskPromptConfig,
  UpsertTemplateRiskPromptConfigPayload,
} from '@/types/template'

export async function getTemplateRiskPromptConfig(
  templateId: string,
): Promise<TemplateRiskPromptConfig> {
  const response = await http.get<ApiEnvelope<TemplateRiskPromptConfig>>(
    `/templates/${templateId}/risk-prompt-config`,
  )
  return unwrapEnvelope(response.data)
}

export async function upsertTemplateRiskPromptConfig(
  templateId: string,
  payload: UpsertTemplateRiskPromptConfigPayload,
): Promise<TemplateRiskPromptConfig> {
  const response = await http.put<ApiEnvelope<TemplateRiskPromptConfig>>(
    `/templates/${templateId}/risk-prompt-config`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function getDecisionFormConfig(templateId: string): Promise<DecisionFormConfig> {
  const response = await http.get<ApiEnvelope<DecisionFormConfig>>(
    `/templates/${templateId}/lifecycle/decision-form-config`,
  )
  return unwrapEnvelope(response.data)
}
