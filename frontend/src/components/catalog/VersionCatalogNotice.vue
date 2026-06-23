<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  kind: 'master' | 'template'
}>()

const { t } = useI18n()

const titleKey = computed(() =>
  props.kind === 'master'
    ? 'versionCatalog.master.noticeTitle'
    : 'versionCatalog.template.noticeTitle',
)
const descriptionKey = computed(() =>
  props.kind === 'master'
    ? 'versionCatalog.master.noticeDescription'
    : 'versionCatalog.template.noticeDescription',
)
</script>

<template>
  <el-alert
    class="catalog-notice"
    type="info"
    :title="t(titleKey)"
    :description="t(descriptionKey)"
    show-icon
    :closable="false"
  />
</template>

<style scoped lang="scss">
.catalog-notice {
  margin-bottom: 1rem;
}
</style>
