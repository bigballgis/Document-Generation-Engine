<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import {
  buildTemplateExportJsonFilename,
  buildTemplateExportZipFilename,
} from '@/utils/parseTemplateExportBundleFile'
import { downloadBlobExport, downloadJsonExport } from '@/utils/downloadExport'

const props = defineProps<{
  templateId: string
  externalId: string
}>()

const { t, te } = useI18n()
const panelDataStore = useTemplatePanelDataStore()
const exporting = computed(() => panelDataStore.getEntry(props.templateId).exporting)

function resolveErrorMessage(error: unknown, fallbackKey: string): string {
  if (error instanceof Error && te(error.message)) {
    return t(error.message)
  }
  return t(fallbackKey)
}

async function handleExportJson() {
  try {
    const result = await panelDataStore.exportTemplateJson(props.templateId)
    downloadJsonExport(
      buildTemplateExportJsonFilename(props.externalId),
      result,
    )
    ElMessage.success(t('templates.export.success'))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'templates.error.export'))
  }
}

async function handleExportZip() {
  try {
    const { blob, filename } = await panelDataStore.exportTemplateZip(props.templateId)
    downloadBlobExport(filename || buildTemplateExportZipFilename(props.externalId), blob)
    ElMessage.success(t('templates.export.success'))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'templates.error.export'))
  }
}

defineExpose({
  handleExportJson,
  handleExportZip,
})
</script>

<template>
  <el-dropdown trigger="click" @command="(command: 'json' | 'zip') => command === 'json' ? handleExportJson() : handleExportZip()">
    <el-button :loading="exporting">
      {{ t('templates.export.action') }}
      <span class="dropdown-caret" aria-hidden="true">▾</span>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="json">
          {{ t('templates.export.json') }}
        </el-dropdown-item>
        <el-dropdown-item command="zip">
          {{ t('templates.export.zip') }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped lang="scss">
.dropdown-caret {
  margin-left: 0.35rem;
  font-size: 0.75rem;
}
</style>
