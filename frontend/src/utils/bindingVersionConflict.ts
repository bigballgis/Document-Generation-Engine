import { ElMessageBox } from 'element-plus'
import { resolveApiError } from '@/api/errorEnvelope'
import type { AnchorBinding } from '@/types/template'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'

export type BindingVersionConflictAction = 'reload' | 'keep' | 'dismiss'

export function isBindingVersionConflict(error: unknown): boolean {
  const resolved = resolveApiError(error)
  if (!resolved) {
    return false
  }
  return (
    resolved.error.messageKey === 'api.error.template.bindingVersionConflict'
    || resolved.error.code === 'BINDING_VERSION_CONFLICT'
  )
}

/**
 * CE-U21 conflict UX — Reload (confirm) vs Keep editing (cancel).
 * Does not clear local drafts; caller owns Reload side-effects.
 */
export async function presentBindingVersionConflict(
  t: (key: string) => string,
): Promise<BindingVersionConflictAction> {
  try {
    await ElMessageBox.confirm(
      t('api.error.template.bindingVersionConflict'),
      t('templates.authoring.bindingVersionConflict'),
      {
        type: 'warning',
        confirmButtonText: t('templates.authoring.bindingVersionConflictReload'),
        cancelButtonText: t('templates.authoring.bindingVersionConflictKeepEditing'),
        distinguishCancelAndClose: true,
        closeOnClickModal: false,
        closeOnPressEscape: true,
      },
    )
    return 'reload'
  } catch (action) {
    if (action === 'cancel') {
      return 'keep'
    }
    return 'dismiss'
  }
}

export type ResolveBindingVersionConflictAndReloadDeps = {
  t: (key: string) => string
  fetchTemplate: () => Promise<unknown>
  onUpdated: () => void
  /** Read after fetchTemplate so store bindings reflect the refreshed template. */
  editingAnchorId: () => string | null | undefined
  editingRow: () => MasterAnchorBindingRow | null
  storeBindings: () => AnchorBinding[] | undefined | null
  reloadBindingFromServer: (row: MasterAnchorBindingRow | null) => void | Promise<void>
}

/**
 * Shared CE-U21 Reload orchestration for Save path and dirty-guard Save path.
 * Keep editing / dismiss leave local drafts and dirty state untouched.
 */
export async function resolveBindingVersionConflictAndReload(
  deps: ResolveBindingVersionConflictAndReloadDeps,
): Promise<BindingVersionConflictAction> {
  const action = await presentBindingVersionConflict(deps.t)
  if (action !== 'reload') {
    return action
  }
  await deps.fetchTemplate()
  deps.onUpdated()
  const fromStore = deps.storeBindings()?.find(
    (item) => item.anchorId === deps.editingAnchorId(),
  )
  const row = deps.editingRow()
  if (fromStore && row) {
    await deps.reloadBindingFromServer({ ...row, binding: fromStore })
  } else {
    await deps.reloadBindingFromServer(row)
  }
  return action
}
