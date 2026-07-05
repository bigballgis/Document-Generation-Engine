/**
 * LR-C1: dirty-form guard composable. Tracks dirty state and blocks navigation
 * (route change, dialog close, tab close) when there are unsaved edits, prompting
 * the user to stay or discard.
 *
 * - `onBeforeRouteLeave` for in-app route changes.
 * - `beforeunload` for tab/window close.
 * - `guardDialogClose` for dialog/panel close.
 *
 * The composable does NOT store dirty state globally — each view owns its own instance.
 */
import { onBeforeUnmount, onMounted, readonly, ref, type Ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'

export interface UseDirtyGuardOptions {
  /** i18n key for the confirm dialog title. Defaults to common.dirtyGuard.title. */
  titleKey?: string
  /** i18n key for the confirm dialog message. Defaults to common.dirtyGuard.message. */
  messageKey?: string
  /** Called when the user chooses to discard. Use to reset the form. */
  onDiscard?: () => void
}

export function useDirtyGuard(dirty: Ref<boolean>, options: UseDirtyGuardOptions = {}) {
  const { t, te } = useI18n()
  const titleKey = options.titleKey ?? 'common.dirtyGuard.title'
  const messageKey = options.messageKey ?? 'common.dirtyGuard.message'
  const suppressed = ref(false)

  function resolveTitle(): string {
    return te(titleKey) ? t(titleKey) : 'Unsaved changes'
  }

  function resolveMessage(): string {
    return te(messageKey) ? t(messageKey) : 'You have unsaved changes. Leave and discard them?'
  }

  async function confirmDiscard(): Promise<boolean> {
    if (!dirty.value || suppressed.value) {
      return true
    }
    try {
      await ElMessageBox.confirm(resolveMessage(), resolveTitle(), {
        confirmButtonText: te('common.dirtyGuard.discard') ? t('common.dirtyGuard.discard') : 'Discard',
        cancelButtonText: te('common.dirtyGuard.stay') ? t('common.dirtyGuard.stay') : 'Stay',
        type: 'warning',
      })
      return true
    } catch {
      return false
    }
  }

  // Block in-app route changes.
  onBeforeRouteLeave(async () => {
    const allow = await confirmDiscard()
    if (allow && options.onDiscard) {
      options.onDiscard()
    }
    return allow
  })

  // Block tab/window close.
  function handleBeforeUnload(event: BeforeUnloadEvent) {
    if (dirty.value && !suppressed.value) {
      event.preventDefault()
      event.returnValue = ''
    }
  }

  onMounted(() => window.addEventListener('beforeunload', handleBeforeUnload))
  onBeforeUnmount(() => window.removeEventListener('beforeunload', handleBeforeUnload))

  /** Guard a dialog/panel close; returns true if the close should proceed. */
  async function guardDialogClose(): Promise<boolean> {
    const allow = await confirmDiscard()
    if (allow && options.onDiscard) {
      options.onDiscard()
    }
    return allow
  }

  /** Temporarily suppress the guard (e.g. right before a programmatic navigation after save). */
  function suppress() {
    suppressed.value = true
  }

  function resume() {
    suppressed.value = false
  }

  return {
    isGuarded: readonly(dirty),
    confirmDiscard,
    guardDialogClose,
    suppress,
    resume,
  }
}
