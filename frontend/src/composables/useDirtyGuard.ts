import { computed, onMounted, onUnmounted, ref, unref, type MaybeRef } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'

export type DirtyGuardDecision = 'stay' | 'discard' | 'save'

export type DirtyGuardPendingAction = () => void | Promise<void>

export interface UseDirtyGuardOptions {
  isDirty: MaybeRef<boolean>
  canSave?: MaybeRef<boolean>
  onSave?: () => Promise<boolean>
  enabled?: MaybeRef<boolean>
}

export function useDirtyGuard(options: UseDirtyGuardOptions) {
  const dialogVisible = ref(false)
  const saving = ref(false)

  const showSaveAction = computed(
    () => Boolean(options.onSave) && unref(options.canSave ?? true),
  )

  let routeLeaveNext: ((allow: boolean) => void) | null = null
  let pendingAction: DirtyGuardPendingAction | null = null

  function isGuardActive(): boolean {
    return unref(options.enabled ?? true) && unref(options.isDirty)
  }

  function handleBeforeUnload(event: BeforeUnloadEvent) {
    if (!isGuardActive()) {
      return
    }
    event.preventDefault()
    event.returnValue = ''
  }

  onMounted(() => {
    window.addEventListener('beforeunload', handleBeforeUnload)
  })

  onUnmounted(() => {
    window.removeEventListener('beforeunload', handleBeforeUnload)
  })

  onBeforeRouteLeave((_to, _from, next) => {
    if (!isGuardActive()) {
      next()
      return
    }
    dialogVisible.value = true
    routeLeaveNext = (allow) => {
      next(allow)
    }
    next(false)
  })

  function resetPendingState() {
    routeLeaveNext = null
    pendingAction = null
  }

  function handleStay() {
    dialogVisible.value = false
    if (routeLeaveNext) {
      routeLeaveNext(false)
    }
    resetPendingState()
  }

  async function handleDiscard() {
    dialogVisible.value = false
    const action = pendingAction
    if (routeLeaveNext) {
      routeLeaveNext(true)
    }
    resetPendingState()
    if (action) {
      await action()
    }
  }

  async function handleSave() {
    if (!options.onSave) {
      return
    }
    saving.value = true
    try {
      const succeeded = await options.onSave()
      if (!succeeded) {
        return
      }
      dialogVisible.value = false
      const action = pendingAction
      if (routeLeaveNext) {
        routeLeaveNext(true)
      }
      resetPendingState()
      if (action) {
        await action()
      }
    } finally {
      saving.value = false
    }
  }

  /**
   * Runs {@link action} immediately when pristine; otherwise opens the dirty-guard dialog first.
   */
  async function requestLeave(action: DirtyGuardPendingAction): Promise<void> {
    if (!isGuardActive()) {
      await action()
      return
    }
    pendingAction = action
    dialogVisible.value = true
  }

  /**
   * Dialog-close helper: returns true when the close may proceed without prompting.
   */
  async function guardDialogClose(onProceed: DirtyGuardPendingAction): Promise<boolean> {
    if (!isGuardActive()) {
      await onProceed()
      return true
    }
    await requestLeave(onProceed)
    return false
  }

  function markPristine() {
    dialogVisible.value = false
    resetPendingState()
  }

  return {
    dialogVisible,
    saving,
    showSaveAction,
    handleStay,
    handleDiscard,
    handleSave,
    requestLeave,
    guardDialogClose,
    markPristine,
    isGuardActive,
  }
}
