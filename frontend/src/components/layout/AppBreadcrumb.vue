<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { buildBreadcrumbTrail } from '@/navigation/breadcrumbTrail'

const props = withDefaults(defineProps<{ hideLastSegment?: boolean }>(), {
  hideLastSegment: false,
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const segments = computed(() => {
  const all = buildBreadcrumbTrail(route.path)
  return props.hideLastSegment && all.length > 1 ? all.slice(0, -1) : all
})

function navigate(path: string) {
  router.push(path)
}
</script>

<template>
  <nav class="app-breadcrumb" :aria-label="t('nav.breadcrumb.ariaLabel')">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item
        v-for="(segment, index) in segments"
        :key="`${segment.labelKey}-${index}`"
      >
        <button
          v-if="segment.path && index < segments.length - 1"
          type="button"
          class="breadcrumb-link"
          @click="navigate(segment.path)"
        >
          {{ t(segment.labelKey) }}
        </button>
        <span v-else>{{ t(segment.labelKey) }}</span>
      </el-breadcrumb-item>
    </el-breadcrumb>
  </nav>
</template>

<style scoped lang="scss">
.app-breadcrumb {
  margin-bottom: 1rem;
}

.breadcrumb-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--brand-primary);
  cursor: pointer;
  font: inherit;

  &:focus-visible {
    outline: 2px solid var(--brand-primary);
    outline-offset: 2px;
  }
}
</style>
