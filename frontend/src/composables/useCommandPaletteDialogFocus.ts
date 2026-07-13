import { nextTick, onUnmounted, ref, watch, type Ref } from 'vue'

export function useCommandPaletteDialogFocus(options: {
  open: Ref<boolean>
  focusNonce: Ref<number>
}) {
  const dialogRef = ref<HTMLElement | null>(null)
  const inputRef = ref<HTMLInputElement | null>(null)

  function collectFocusable(root: HTMLElement): HTMLElement[] {
    const nodes = root.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
    )
    return Array.from(nodes).filter((el) => !el.hasAttribute('disabled') && el.tabIndex !== -1)
  }

  function onDialogKeydown(event: KeyboardEvent) {
    if (event.key !== 'Tab' || !dialogRef.value) {
      return
    }
    const focusables = collectFocusable(dialogRef.value)
    if (focusables.length === 0) {
      event.preventDefault()
      return
    }
    const first = focusables[0]!
    const last = focusables[focusables.length - 1]!
    const active = document.activeElement as HTMLElement | null
    if (event.shiftKey) {
      if (active === first || !dialogRef.value.contains(active)) {
        event.preventDefault()
        last.focus()
      }
    } else if (active === last) {
      event.preventDefault()
      first.focus()
    }
  }

  watch(options.open, async (isOpen) => {
    if (!isOpen) {
      if (dialogRef.value) {
        dialogRef.value.removeEventListener('keydown', onDialogKeydown)
      }
      return
    }
    await nextTick()
    inputRef.value?.focus()
    dialogRef.value?.addEventListener('keydown', onDialogKeydown)
  })

  watch(options.focusNonce, async () => {
    if (!options.open.value) {
      return
    }
    await nextTick()
    inputRef.value?.focus()
  })

  onUnmounted(() => {
    dialogRef.value?.removeEventListener('keydown', onDialogKeydown)
  })

  return {
    dialogRef,
    inputRef,
  }
}
