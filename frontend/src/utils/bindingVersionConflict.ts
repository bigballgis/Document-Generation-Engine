import { ElMessageBox } from 'element-plus'
import { resolveApiError } from '@/api/errorEnvelope'

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
