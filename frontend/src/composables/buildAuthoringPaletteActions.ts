import type { AuthoringEditorContext } from '@/composables/authoringEditorContext'
import type { PaletteItem } from '@/composables/commandPaletteTypes'

export type ShortcutPlatform = 'mac' | 'other'

function detectShortcutPlatform(
  userAgent: string = typeof navigator !== 'undefined' ? navigator.userAgent : '',
  platform: string = typeof navigator !== 'undefined' ? navigator.platform : '',
): ShortcutPlatform {
  if (/Mac|iPhone|iPad|iPod/i.test(platform) || /Mac OS|Macintosh/i.test(userAgent)) {
    return 'mac'
  }
  return 'other'
}

export function formatModShortcut(key: string, platform: ShortcutPlatform = detectShortcutPlatform()): string {
  return platform === 'mac' ? `⌘${key.toUpperCase()}` : `Ctrl+${key.toUpperCase()}`
}

/** Build command-palette Actions for the active bindings editor (CE-U17). */
export function buildAuthoringPaletteActions(
  context: AuthoringEditorContext | null,
  translate: (key: string) => string,
  platform: ShortcutPlatform = detectShortcutPlatform(),
): PaletteItem[] {
  if (!context) {
    return []
  }

  const items: PaletteItem[] = []

  if (context.canSave()) {
    items.push({
      id: 'action:save-binding',
      kind: 'action',
      title: translate('commandPalette.actions.saveBinding'),
      subtitle: formatModShortcut('S', platform),
      target: { path: '' },
      optionTestId: 'command-palette-action-save-binding',
      execute: async () => {
        if (!context.canSave() || context.isSaving()) {
          return
        }
        await context.saveBinding()
      },
    })
  }

  if (context.canRefresh()) {
    items.push({
      id: 'action:refresh-preview',
      kind: 'action',
      title: translate('commandPalette.actions.refreshPreview'),
      subtitle: formatModShortcut('P', platform),
      target: { path: '' },
      optionTestId: 'command-palette-action-refresh-preview',
      execute: async () => {
        if (!context.canRefresh() || context.isRefreshing()) {
          return
        }
        await context.refreshPreview()
      },
    })
  }

  return items
}
