/** IBL-E4 / ADR-0065 — DocumentBrand catalog (≠ UI BrandPreset REDBC/GREENBC). */

export type DocumentBrandStatus = 'ACTIVE' | 'INACTIVE'

export interface DocumentBrandView {
  groupCode: string
  documentBrandCode: string
  displayName: string
  status: DocumentBrandStatus
  logoObjectRef: string
  defaultSealObjectRef?: string | null
  letterheadLegalName?: string | null
}

export interface CreateDocumentBrandPayload {
  groupCode: string
  documentBrandCode: string
  displayName: string
  status?: DocumentBrandStatus
  logoObjectRef: string
  defaultSealObjectRef?: string | null
  letterheadLegalName?: string | null
}

export interface UpdateDocumentBrandPayload {
  groupCode: string
  displayName?: string
  status?: DocumentBrandStatus
  logoObjectRef?: string
  defaultSealObjectRef?: string | null
  letterheadLegalName?: string | null
}

export interface LegalEntityView {
  groupCode: string
  legalEntityCode: string
  displayName: string
  status: DocumentBrandStatus
  documentBrandCode: string
}

export interface CreateLegalEntityPayload {
  groupCode: string
  legalEntityCode: string
  displayName: string
  status?: DocumentBrandStatus
  documentBrandCode: string
}

export interface UpdateLegalEntityPayload {
  groupCode: string
  displayName?: string
  status?: DocumentBrandStatus
  documentBrandCode?: string
}

export interface GroupDefaultLegalEntityView {
  groupCode: string
  defaultLegalEntityCode: string | null
}

export interface PutGroupDefaultLegalEntityPayload {
  defaultLegalEntityCode: string | null
}
