import { onMounted, onUnmounted } from 'vue'
import {
  getAuthoringEditorContext,
  isAriaModalOpen,
} from '@/composables/authoringEditorContext'

function isModKey(event: KeyboardEvent): boolean {
  return (event.ctrlKey || event.metaKey) && !event.altKey
}

function handleAuthoringShortcutKeydown(event: KeyboardEvent): void {
  const context = getAuthoringEditorContext()
  if (!context) {
    return
  }

  const key = event.key.length === 1 ? event.key.toLowerCase() : event.key
  const isSave = isModKey(event) && key === 's'
  const isRefresh = isModKey(event) && key === 'p'
  if (!isSave && !isRefresh) {
    return
  }

  // Always block browser Save Page / Print while the bindings editor is active.
  event.preventDefault()
  event.stopPropagation()

  if (isAriaModalOpen()) {
    return
  }

  if (isSave) {
    if (!context.canSave() || context.isSaving()) {
      return
    }
    void context.saveBinding()
    return
  }

  if (!context.canRefresh() || context.isRefreshing()) {
    return
  }
  void context.refreshPreview()
}

export interface UseAuthoringEditorShortcutsOptions {
  /** Injected for tests; defaults to document keydown. */
  bindShortcut?: boolean
}

/**
 * Document-level Ctrl/Cmd+S (save binding) and Ctrl/Cmd+P (refresh preview).
 * Only acts when {@link registerAuthoringEditorContext} has an active edit surface.
 */
export function useAuthoringEditorShortcuts(options: UseAuthoringEditorShortcutsOptions = {}) {
  if (options.bindShortcut === false) {
    return { handleAuthoringShortcutKeydown }
  }

  onMounted(() => {
    document.addEventListener('keydown', handleAuthoringShortcutKeydown)
  })
  onUnmounted(() => {
    document.removeEventListener('keydown', handleAuthoringShortcutKeydown)
  })

  return { handleAuthoringShortcutKeydown }
}

export { handleAuthoringShortcutKeydown }
