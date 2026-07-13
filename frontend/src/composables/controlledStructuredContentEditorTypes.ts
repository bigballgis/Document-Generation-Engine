import type {
  MasterStyleCatalog,
  PasteCleaningEvidence,
  VariableSchema,
} from '@/types/template'
import type { ConfirmedNodeType } from '@/utils/structuredContentNodes'

export interface ControlledStructuredContentEditorProps {
  modelValue: string
  templateId?: string
  /** Dev-version scope for local draft keys (LR-C2). When absent, drafts are disabled. */
  devVersionId?: string
  /** Optional binding anchor for draft recovery disambiguation. */
  anchorId?: string
  /** Optional server revision timestamp shown on the recovery banner. */
  serverUpdatedAt?: string | null
  variableKeys?: string[]
  variables?: VariableSchema[]
  contentModuleReferenceKeys?: string[]
  readonly?: boolean
  /** Saved baseline JSON; when omitted, initial modelValue is the baseline. */
  baseline?: string
}

export type ControlledStructuredContentEditorEmit = {
  (event: 'update:modelValue', value: string): void
  (event: 'dirty-change', dirty: boolean): void
  (event: 'structure-change'): void
  /** Fired on Accept with non-sensitive residue for binding upsert (blockedCount=0). */
  (event: 'paste-accepted', evidence: PasteCleaningEvidence): void
}

export const DEFAULT_STYLE_CATALOG: MasterStyleCatalog = {
  catalogVersion: '1.0',
  entries: [
    { styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' },
    { styleKey: 'Heading1', applicableNodeTypes: ['sectionHeading'], renderPurpose: 'HEADING' },
  ],
}

export const STRUCTURED_BLOCK_NODE_TYPES: ConfirmedNodeType[] = [
  'sectionHeading',
  'paragraph',
  'list',
  'conditionBlock',
  'loopBlock',
  'tableComponentRef',
  'contentModuleRef',
]
