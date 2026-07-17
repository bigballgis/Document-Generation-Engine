import { shallowRef, type ShallowRef } from 'vue'

/** Active Authoring Bindings editor surface (CE-U17). Null outside edit mode. */
export interface AuthoringEditorContext {
  saveBinding: () => void | Promise<void>
  refreshPreview: () => void | Promise<void>
  canSave: () => boolean
  canRefresh: () => boolean
  isSaving: () => boolean
  isRefreshing: () => boolean
}

const authoringEditorContextRef: ShallowRef<AuthoringEditorContext | null> = shallowRef(null)

export function getAuthoringEditorContext(): AuthoringEditorContext | null {
  return authoringEditorContextRef.value
}

export function useAuthoringEditorContextRef(): ShallowRef<AuthoringEditorContext | null> {
  return authoringEditorContextRef
}

/** Register edit-surface handlers; returns unregister (idempotent). */
export function registerAuthoringEditorContext(context: AuthoringEditorContext): () => void {
  authoringEditorContextRef.value = context
  return () => {
    if (authoringEditorContextRef.value === context) {
      authoringEditorContextRef.value = null
    }
  }
}

/** Test helper — clear registry between cases. */
export function clearAuthoringEditorContext(): void {
  authoringEditorContextRef.value = null
}

/**
 * True when a modal/dialog focus trap is actually presented (incl. command palette).
 *
 * Element Plus keeps closed `el-dialog` nodes in the DOM with `aria-modal="true"` under
 * a `display: none` overlay — those must not suppress author shortcuts (CE-U17 / U17-D8).
 */
export function isAriaModalOpen(root: ParentNode = document): boolean {
  const modals = root.querySelectorAll('[aria-modal="true"]')
  for (const node of modals) {
    if (node instanceof HTMLElement && isPresentedModalElement(node)) {
      return true
    }
  }
  return false
}

function isPresentedModalElement(el: HTMLElement): boolean {
  // Walk ancestors: closed Element Plus dialogs sit under `.el-overlay { display: none }`.
  // Avoid getBoundingClientRect — jsdom returns 0×0 for styled nodes and would false-negative.
  let current: HTMLElement | null = el
  while (current) {
    const style = window.getComputedStyle(current)
    if (style.display === 'none' || style.visibility === 'hidden') {
      return false
    }
    current = current.parentElement
  }
  return true
}
