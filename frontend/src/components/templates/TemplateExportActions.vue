<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import * as templatesApi from '@/api/templates'
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
const exporting = ref(false)

function resolveErrorMessage(error: unknown, fallbackKey: string): string {
  if (error instanceof Error && te(error.message)) {
    return t(error.message)
  }
  return t(fallbackKey)
}

async function handleExportJson() {
  exporting.value = true
  try {
    const result = await templatesApi.exportTemplateJson(props.templateId)
    downloadJsonExport(
      buildTemplateExportJsonFilename(props.externalId),
      result,
    )
    ElMessage.success(t('templates.export.success'))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'templates.error.export'))
  } finally {
    exporting.value = false
  }
}

async function handleExportZip() {
  exporting.value = true
  try {
    const { blob, filename } = await templatesApi.exportTemplateZip(props.templateId)
    downloadBlobExport(filename || buildTemplateExportZipFilename(props.externalId), blob)
    ElMessage.success(t('templates.export.success'))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'templates.error.export'))
  } finally {
    exporting.value = false
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
