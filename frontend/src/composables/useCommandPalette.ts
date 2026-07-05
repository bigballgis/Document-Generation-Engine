/**
 * LR-C6: global search / command palette (Ctrl+K). A keyboard-first navigation surface that
 * searches templates, masters, content modules, and nav destinations, then routes on select.
 *
 * The palette is pure frontend — it queries the existing list endpoints with the `search`
 * param (LR-C5) and routes via vue-router. No new backend search index is introduced in v1.
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTemplatesStore } from '@/stores/templates'

export interface SearchEntry {
  id: string
  label: string
  description?: string
  category: 'template' | 'master' | 'contentModule' | 'navigation'
  routePath: string
}

export function useCommandPalette() {
  const open = ref(false)
  const query = ref('')
  const results = ref<SearchEntry[]>([])
  const loading = ref(false)

  const router = useRouter()

  function openPalette() {
    open.value = true
    query.value = ''
    results.value = []
  }

  function closePalette() {
    open.value = false
  }

  function togglePalette() {
    if (open.value) {
      closePalette()
    } else {
      openPalette()
    }
  }

  async function search(term: string) {
    query.value = term
    if (!term.trim()) {
      results.value = []
      return
    }
    loading.value = true
    try {
      const templatesStore = useTemplatesStore()
      await templatesStore.fetchTemplates(0, 10, { search: term.trim() })
      results.value = templatesStore.templates.slice(0, 10).map<SearchEntry>((t) => ({
        id: `template:${t.id}`,
        label: t.name,
        description: t.externalId,
        category: 'template',
        routePath: `/templates/${t.id}`,
      }))
    } catch {
      results.value = []
    } finally {
      loading.value = false
    }
  }

  async function selectEntry(entry: SearchEntry) {
    closePalette()
    await router.push(entry.routePath)
  }

  const hasResults = computed(() => results.value.length > 0)

  return {
    open,
    query,
    results,
    loading,
    hasResults,
    openPalette,
    closePalette,
    togglePalette,
    search,
    selectEntry,
  }
}
