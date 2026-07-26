<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import * as collaborationApi from '@/api/collaboration'
import { useCapabilities } from '@/composables/useCapabilities'
import { canMaintainCollaborationTimeoutConfig } from '@/auth/roles'
import { useSessionStore } from '@/stores/session'
import type {
  CollaborationTimeoutConfig,
  CollaborationTimeoutScopeType,
} from '@/types/collaboration'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'

const props = defineProps<{
  /** Locked IA mode — global page or group dialog (no scope switcher). */
  mode: 'global' | 'group'
  groupCode?: string | null
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const { context } = useCapabilities()
const sessionStore = useSessionStore()

const loading = ref(false)
const saving = ref(false)
const config = ref<CollaborationTimeoutConfig | null>(null)

const canMaintain = computed(() => canMaintainCollaborationTimeoutConfig(context.value))
const lockedScope = computed<CollaborationTimeoutScopeType>(() =>
  props.mode === 'global' ? 'GLOBAL' : 'GROUP',
)

const form = reactive({
  groupCode: props.groupCode ?? '',
  testThresholdHours: 72,
  approvalThresholdHours: 72,
  pendingReleaseThresholdHours: 48,
  remediationThresholdHours: 168,
})

function resolveGroupCode(): string | undefined {
  if (lockedScope.value !== 'GROUP') {
    return undefined
  }
  return form.groupCode || props.groupCode || undefined
}

async function loadConfig() {
  if (!canMaintain.value) {
    return
  }
  loading.value = true
  try {
    config.value = await collaborationApi.getCollaborationTimeoutConfig(resolveGroupCode())
    form.groupCode = config.value.groupCode ?? form.groupCode
    form.testThresholdHours = config.value.testThresholdHours
    form.approvalThresholdHours = config.value.approvalThresholdHours
    form.pendingReleaseThresholdHours = config.value.pendingReleaseThresholdHours
    form.remediationThresholdHours = config.value.remediationThresholdHours
  } catch {
    ElMessage.error(t('collaboration.timeoutConfig.error.load'))
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  saving.value = true
  try {
    config.value = await collaborationApi.upsertCollaborationTimeoutConfig({
      scopeType: lockedScope.value,
      groupCode: lockedScope.value === 'GROUP' ? resolveGroupCode() ?? null : null,
      testThresholdHours: form.testThresholdHours,
      approvalThresholdHours: form.approvalThresholdHours,
      pendingReleaseThresholdHours: form.pendingReleaseThresholdHours,
      remediationThresholdHours: form.remediationThresholdHours,
    })
    ElMessage.success(t('collaboration.timeoutConfig.saveSuccess'))
  } catch {
    ElMessage.error(t('collaboration.timeoutConfig.error.save'))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (props.mode === 'group') {
    form.groupCode =
      props.groupCode ??
      sessionStore.session?.authorizedGroupCodes.find((code) => code !== '*') ??
      sessionStore.session?.authorizedGroupCodes[0] ??
      ''
  }
  void loadConfig()
})
</script>

<template>
  <el-card v-if="canMaintain" shadow="never" class="timeout-config-card">
    <div class="timeout-config-header">
      <div>
        <h2>{{ t('collaboration.timeoutConfig.title') }}</h2>
        <p>{{ t('collaboration.timeoutConfig.description') }}</p>
      </div>
      <el-button :loading="loading" @click="loadConfig">
        {{ t('collaboration.timeoutConfig.refresh') }}
      </el-button>
    </div>

    <div v-loading="loading" class="timeout-config-body">
      <el-form label-position="top">
        <el-form-item
          v-if="mode === 'group'"
          :label="t('collaboration.timeoutConfig.groupCode')"
        >
          <el-input
            v-model="form.groupCode"
            readonly
            :placeholder="t('collaboration.timeoutConfig.groupCodePlaceholder')"
          />
        </el-form-item>

        <div class="threshold-grid">
          <el-form-item :label="t('collaboration.timeoutConfig.testThresholdHours')">
            <el-input-number v-model="form.testThresholdHours" :min="1" :max="8760" />
          </el-form-item>
          <el-form-item :label="t('collaboration.timeoutConfig.approvalThresholdHours')">
            <el-input-number v-model="form.approvalThresholdHours" :min="1" :max="8760" />
          </el-form-item>
          <el-form-item :label="t('collaboration.timeoutConfig.pendingReleaseThresholdHours')">
            <el-input-number v-model="form.pendingReleaseThresholdHours" :min="1" :max="8760" />
          </el-form-item>
          <el-form-item :label="t('collaboration.timeoutConfig.remediationThresholdHours')">
            <el-input-number v-model="form.remediationThresholdHours" :min="1" :max="8760" />
          </el-form-item>
        </div>
      </el-form>

      <p v-if="config?.updatedAt" class="updated-at" data-testid="timeout-config-updated-at">
        {{
          t('collaboration.timeoutConfig.lastUpdated', {
            updatedAt: formatDateTime(config.updatedAt),
          })
        }}
      </p>

      <el-button type="primary" :loading="saving" @click="saveConfig">
        {{ t('collaboration.timeoutConfig.save') }}
      </el-button>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.timeout-config-card {
  margin-top: 0;
}

.timeout-config-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;

  h2 {
    margin: 0 0 0.35rem;
    font-size: 1.125rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.threshold-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.75rem 1.25rem;
}

.updated-at {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
