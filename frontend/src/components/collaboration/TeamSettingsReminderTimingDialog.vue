<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import CollaborationTimeoutConfigPanel from '@/components/collaboration/CollaborationTimeoutConfigPanel.vue'
import { useSessionStore } from '@/stores/session'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { t } = useI18n()
const sessionStore = useSessionStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const groupCode = computed(() => {
  const codes = sessionStore.session?.authorizedGroupCodes ?? []
  return codes.find((code) => code !== '*') ?? codes[0] ?? ''
})
</script>

<template>
  <el-dialog
    v-model="visible"
    class="team-settings-dialog"
    :title="t('identity.groups.teamSettings')"
    width="720px"
    destroy-on-close
    append-to-body
  >
    <CollaborationTimeoutConfigPanel v-if="visible" mode="group" :group-code="groupCode" />
  </el-dialog>
</template>
