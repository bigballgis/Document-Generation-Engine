<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { listDocumentBrands } from '@/api/documentBrands'
import type { DocumentBrandView } from '@/types/documentBrand'

const props = defineProps<{
  modelValue: string[]
  groupCode: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const { t } = useI18n()
const brandOptions = ref<DocumentBrandView[]>([])
const loading = ref(false)

const selected = computed({
  get: () => props.modelValue,
  set: (value: string[]) => emit('update:modelValue', value),
})

async function loadBrands() {
  if (!props.groupCode) {
    brandOptions.value = []
    return
  }
  loading.value = true
  try {
    const page = await listDocumentBrands(props.groupCode, { status: 'ACTIVE' })
    brandOptions.value = page.content
  } catch {
    brandOptions.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.groupCode,
  () => {
    void loadBrands()
  },
)

onMounted(() => {
  void loadBrands()
})
</script>

<template>
  <div class="document-brand-allow-list" data-testid="template-document-brand-allow-list">
    <el-form-item :label="t('templates.documentBrandAllowList.label')">
      <el-select
        v-model="selected"
        multiple
        clearable
        filterable
        collapse-tags
        collapse-tags-tooltip
        class="document-brand-allow-list__select"
        data-testid="template-document-brand-allow-list-select"
        :disabled="disabled"
        :loading="loading"
        :placeholder="t('templates.documentBrandAllowList.placeholder')"
      >
        <el-option
          v-for="brand in brandOptions"
          :key="brand.documentBrandCode"
          :label="`${brand.displayName} (${brand.documentBrandCode})`"
          :value="brand.documentBrandCode"
        />
      </el-select>
      <p class="document-brand-allow-list__hint">
        {{ t('templates.documentBrandAllowList.hint') }}
      </p>
    </el-form-item>
  </div>
</template>

<style scoped lang="scss">
.document-brand-allow-list {
  &__select {
    width: 100%;
  }

  &__hint {
    margin: 0.35rem 0 0;
    font-size: 0.85rem;
    color: var(--text-muted);
    line-height: 1.4;
  }
}
</style>
