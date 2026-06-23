import { ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'

export interface ConfirmActionOptions {
  titleKey: string
  messageKey: string
  confirmButtonKey?: string
  cancelButtonKey?: string
  type?: 'warning' | 'info' | 'success' | 'error'
}

export function useConfirmAction() {
  const { t } = useI18n()

  async function confirmAction(options: ConfirmActionOptions): Promise<boolean> {
    try {
      await ElMessageBox.confirm(t(options.messageKey), t(options.titleKey), {
        confirmButtonText: t(options.confirmButtonKey ?? 'common.confirm'),
        cancelButtonText: t(options.cancelButtonKey ?? 'common.cancel'),
        type: options.type ?? 'warning',
      })
      return true
    } catch {
      return false
    }
  }

  return { confirmAction }
}
