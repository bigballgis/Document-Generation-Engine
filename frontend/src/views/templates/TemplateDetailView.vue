<script setup lang="ts">
import { reactive, toRef } from 'vue'
import TemplateDetailViewBody from '@/views/templates/detail/TemplateDetailViewBody.vue'
import { useTemplateDetailController } from './useTemplateDetailController'

const props = withDefaults(
  defineProps<{
    workspace?: 'legacy' | 'dev-editor'
  }>(),
  {
    workspace: 'legacy',
  },
)

/** Reactive wrap keeps nested refs auto-unwrapped in template (thin orchestrator shell). */
const c = reactive(useTemplateDetailController(toRef(props, 'workspace')))
</script>

<template>
  <TemplateDetailViewBody :c="c" />
</template>
