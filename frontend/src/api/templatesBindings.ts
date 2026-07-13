import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  AnchorBinding,
  BindingValidationResult,
  CompositionRuleInput,
  MasterStyleCatalog,
  PasteCleanResult,
  RuleValidationResult,
  UpsertBindingPayload,
  UpsertVariablePayload,
  VariableSchema,
} from '@/types/template'

export async function validateBindings(templateId: string): Promise<BindingValidationResult> {
  const response = await http.post<ApiEnvelope<BindingValidationResult>>(
    `/templates/${templateId}/bindings/validate`,
    {},
  )
  return unwrapEnvelope(response.data)
}

export async function getMasterStyleCatalog(templateId: string): Promise<MasterStyleCatalog> {
  const response = await http.get<ApiEnvelope<MasterStyleCatalog>>(
    `/templates/${templateId}/master-style-catalog`,
  )
  return unwrapEnvelope(response.data)
}

export async function pasteClean(
  templateId: string,
  payload: { sourceHtml: string; prePasteStructuredContentJson: string },
): Promise<PasteCleanResult> {
  const response = await http.post<ApiEnvelope<PasteCleanResult>>(
    `/templates/${templateId}/paste-clean`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function validateRules(
  templateId: string,
  rules: CompositionRuleInput[],
): Promise<RuleValidationResult> {
  const response = await http.post<ApiEnvelope<RuleValidationResult>>(
    `/templates/${templateId}/rules/validate`,
    { rules },
  )
  return unwrapEnvelope(response.data)
}

export async function saveRules(
  templateId: string,
  rules: CompositionRuleInput[],
): Promise<CompositionRuleInput[]> {
  const response = await http.put<ApiEnvelope<CompositionRuleInput[]>>(
    `/templates/${templateId}/rules`,
    { rules },
  )
  return unwrapEnvelope(response.data)
}

export async function upsertVariable(
  templateId: string,
  variableKey: string,
  payload: UpsertVariablePayload,
): Promise<VariableSchema> {
  const response = await http.put<ApiEnvelope<VariableSchema>>(
    `/templates/${templateId}/variables/${encodeURIComponent(variableKey)}`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function deleteVariable(templateId: string, variableKey: string): Promise<void> {
  await http.delete(`/templates/${templateId}/variables/${encodeURIComponent(variableKey)}`)
}

export async function upsertBinding(
  templateId: string,
  anchorId: string,
  payload: UpsertBindingPayload,
): Promise<AnchorBinding> {
  const response = await http.put<ApiEnvelope<AnchorBinding>>(
    `/templates/${templateId}/bindings/${encodeURIComponent(anchorId)}`,
    payload,
  )
  return unwrapEnvelope(response.data)
}
