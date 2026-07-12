import type {
  AnchorBinding,
  CompositionRule,
  PreviewRecord,
  TemplateContentModuleReference,
  VariableSchema,
} from '@/types/template'

export const BINDING_CONTENT_TYPES = [
  'TEXT',
  'RICH_TEXT',
  'TABLE',
  'IMAGE',
  'CLAUSE',
  'SEAL',
  'QR_CODE',
  'ATTACHMENT_LIST',
] as const

export type BindingPanelMode = 'list' | 'edit'

export type EditSnapshot = {
  declaredContentType: string
  structuredContentJson: string
  visibilityEnabled: boolean
  visibilityExpression: string
}

export type TemplateAuthoringBindingsPanelProps = {
  templateId: string
  masterId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[] | null
  contentModuleReferences: TemplateContentModuleReference[]
  lastPreview?: PreviewRecord | null
  selectedTestDataSetId?: string | null
  generatingPreview?: boolean
}

export type StructuredBindingEditorExpose = {
  markPristine: () => void
}
