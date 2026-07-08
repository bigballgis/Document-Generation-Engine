import { ref } from 'vue'
import { useTemplatesStore } from '@/stores/templates'
import type { TableColumnFilterOption } from '@/composables/useTableFilterOptions'

export function useAuditTemplateFilterOptions() {
  const templatesStore = useTemplatesStore()
  const templateOptions = ref<TableColumnFilterOption[]>([])
  const loadingTemplates = ref(false)

  async function searchTemplates(query: string): Promise<void> {
    loadingTemplates.value = true
    try {
      await templatesStore.fetchTemplates(0, 20, {
        search: query.trim() || undefined,
      })
      templateOptions.value = templatesStore.templates.map((template) => ({
        value: template.id,
        label: `${template.name} (${template.externalId})`,
      }))
    } finally {
      loadingTemplates.value = false
    }
  }

  return {
    templateOptions,
    loadingTemplates,
    searchTemplates,
  }
}
