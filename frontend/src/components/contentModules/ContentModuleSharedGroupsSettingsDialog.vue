<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { useContentModulesStore } from '@/stores/contentModules'
import {
  excludeOwnerFromSharedGroupCodes,
  normalizeSharedGroupCodes,
  sharedGroupSelectionChanged,
} from '@/utils/contentModuleSharedGroups'

const props = defineProps<{
  modelValue: boolean
  moduleId: string
  ownerGroupCode: string
  sharedGroupCodes: string[]
  canConfigure: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const { t, te } = useI18n()
const contentModulesStore = useContentModulesStore()
const { groupOptions, ensureGroupCatalog } = useScopedGroupOptions()

const selectedSharedGroupCodes = ref<string[]>([])
const baselineSharedGroupCodes = ref<string[]>([])

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const sharedGroupSelectOptions = computed(() =>
  groupOptions.value.filter(
    (option) => option.value.toUpperCase() !== props.ownerGroupCode.trim().toUpperCase(),
  ),
)

const apiErrorMessage = computed(() => {
  const key = contentModulesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('contentModules.error.updateSharedGroups')
})

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) {
      return
    }
    await ensureGroupCatalog()
    const normalized = excludeOwnerFromSharedGroupCodes(
      props.sharedGroupCodes,
      props.ownerGroupCode,
    )
    selectedSharedGroupCodes.value = [...normalized]
    baselineSharedGroupCodes.value = [...normalized]
  },
  { immediate: true },
)

async function handleSave() {
  if (!props.canConfigure) {
    return
  }
  const nextCodes = excludeOwnerFromSharedGroupCodes(
    selectedSharedGroupCodes.value,
    props.ownerGroupCode,
  )
  if (sharedGroupSelectionChanged(nextCodes, baselineSharedGroupCodes.value)) {
    try {
      await ElMessageBox.confirm(
        t('contentModules.settings.confirmMessage'),
        t('contentModules.settings.confirmTitle'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'warning',
        },
      )
    } catch {
      return
    }
  }
  try {
    await contentModulesStore.updateSharedGroupCodes(props.moduleId, {
      sharedGroupCodes: normalizeSharedGroupCodes(nextCodes),
    })
    ElMessage.success(t('contentModules.settings.saveSuccess'))
    visible.value = false
    emit('saved')
  } catch {
    // Store surfaces message key.
  }
}

function setSelectedSharedGroupCodes(codes: string[]) {
  selectedSharedGroupCodes.value = [...codes]
}

defineExpose({
  selectedSharedGroupCodes,
  sharedGroupSelectOptions,
  setSelectedSharedGroupCodes,
  handleSave,
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('contentModules.settings.title')"
    width="560px"
    destroy-on-close
  >
    <el-alert
      v-if="apiErrorMessage"
      class="dialog-alert"
      type="error"
      :title="apiErrorMessage"
      show-icon
      :closable="false"
    />
    <p class="settings-owner">
      {{ t('contentModules.detail.summary.owner', { groupCode: ownerGroupCode }) }}
    </p>
    <el-form label-position="top">
      <el-form-item :label="t('contentModules.settings.sharedGroupCodes')">
        <el-select
          v-model="selectedSharedGroupCodes"
          data-testid="content-module-shared-groups-select"
          multiple
          filterable
          clearable
          class="shared-groups-select"
          :disabled="!canConfigure"
          :placeholder="t('contentModules.settings.sharedGroupCodesPlaceholder')"
        >
          <el-option
            v-for="option in sharedGroupSelectOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button
        type="primary"
        data-testid="content-module-shared-groups-save"
        :disabled="!canConfigure"
        :loading="contentModulesStore.submitting"
        @click="handleSave"
      >
        {{ t('contentModules.settings.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dialog-alert {
  margin-bottom: var(--space-4);
}

.settings-owner {
  margin: 0 0 var(--space-4);
  color: var(--text-muted);
}

.shared-groups-select {
  width: 100%;
}
</style>
