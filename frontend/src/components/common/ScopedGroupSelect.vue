<script setup lang="ts">
import { watch } from 'vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'

const model = defineModel<string>({ default: '' })

withDefaults(
  defineProps<{
    placeholder?: string
    clearable?: boolean
    class?: string
  }>(),
  {
    clearable: true,
  },
)

const {
  groupOptions,
  isGroupLocked,
  lockedGroupCode,
  ensureGroupCatalog,
  resolveDefaultGroupCode,
} = useScopedGroupOptions()

watch(
  lockedGroupCode,
  (code) => {
    if (isGroupLocked.value && code) {
      model.value = code
    }
  },
  { immediate: true },
)

async function prepare() {
  await ensureGroupCatalog()
  model.value = resolveDefaultGroupCode(model.value)
}

defineExpose({ prepare, ensureGroupCatalog })
</script>

<template>
  <AppSearchSelect
    v-model="model"
    :class="$props.class"
    :disabled="isGroupLocked"
    :clearable="!isGroupLocked && clearable"
    :placeholder="placeholder"
  >
    <el-option
      v-for="option in groupOptions"
      :key="option.value"
      :label="option.label"
      :value="option.value"
    />
  </AppSearchSelect>
</template>
