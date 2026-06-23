<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  titleKey: string
  descriptionKey?: string
}>()

const { t, te } = useI18n()

const title = computed(() => (te(props.titleKey) ? t(props.titleKey) : props.titleKey))
const description = computed(() =>
  props.descriptionKey && te(props.descriptionKey) ? t(props.descriptionKey) : '',
)
</script>

<template>
  <el-empty :description="title">
    <p v-if="description" class="empty-description">{{ description }}</p>
  </el-empty>
</template>

<style scoped lang="scss">
.empty-description {
  margin: 0.5rem 0 0;
  color: var(--text-muted);
}
</style>
