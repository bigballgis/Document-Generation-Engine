<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getCallerContract } from '@/api/contract'
import {
  ALLOWED_ENVIRONMENTS,
  ENVIRONMENT_LABEL_KEY,
  resolveRuntimeEnvironment,
  type RuntimeEnvironment,
} from '@/config/environments'
import type { CallerContract } from '@/types/contract'

const props = defineProps<{
  templateId: string
  environment?: RuntimeEnvironment
}>()
const emit = defineEmits<{
  'update:environment': [environment: RuntimeEnvironment]
}>()

const { t, te } = useI18n()
const loading = ref(false)
const errorMessageKey = ref<string | null>(null)
const contract = ref<CallerContract | null>(null)

const selectedEnvironment = ref<RuntimeEnvironment>(resolveRuntimeEnvironment(props.environment))
const environmentOptions = computed(() =>
  ALLOWED_ENVIRONMENTS.map((environment) => ({
    value: environment,
    label: t(ENVIRONMENT_LABEL_KEY[environment]),
  })),
)
const currentEnvironment = computed(() => selectedEnvironment.value)

const versionComparison = computed(() => {
  if (!contract.value) {
    return []
  }
  const defaultVersion = contract.value.defaultRoute.currentTargetReleaseVersion
  return contract.value.callableVersions.map((version) => ({
    releaseVersion: version.releaseVersion,
    explicitVersionUrl: version.explicitVersionUrl,
    isDefaultRouteTarget: version.releaseVersion === defaultVersion,
  }))
})

async function loadContract() {
  loading.value = true
  errorMessageKey.value = null
  try {
    contract.value = await getCallerContract(props.templateId, currentEnvironment.value)
  } catch {
    errorMessageKey.value = 'templates.contract.error.load'
  } finally {
    loading.value = false
  }
}

watch(
  () => props.environment,
  (value) => {
    const resolved = resolveRuntimeEnvironment(value)
    if (selectedEnvironment.value !== resolved) {
      selectedEnvironment.value = resolved
    }
  },
  { immediate: true },
)

watch(
  selectedEnvironment,
  (value) => {
    emit('update:environment', value)
  },
)

watch(
  [() => props.templateId, currentEnvironment],
  () => {
    void loadContract()
  },
  { immediate: true },
)

function errorMessage(key: string | null): string {
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.contract.error.load')
}
</script>

<template>
  <div class="caller-contract-panel">
    <el-skeleton v-if="loading" :rows="6" animated />

    <el-alert
      v-else-if="errorMessageKey"
      type="error"
      :title="errorMessage(errorMessageKey)"
      show-icon
      :closable="false"
    />

    <template v-else-if="contract">
      <div class="toolbar">
        <el-form-item :label="t('templates.contract.environment')">
          <el-select v-model="selectedEnvironment" class="environment-select">
            <el-option
              v-for="option in environmentOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
      </div>

      <p class="panel-description">{{ t('templates.contract.description') }}</p>

      <h3>{{ t('templates.contract.sections.paths') }}</h3>
      <ul class="path-list">
        <li v-for="path in contract.paths" :key="path">
          <code>{{ path }}</code>
        </li>
      </ul>

      <h3>{{ t('templates.contract.sections.versions') }}</h3>
      <el-table :data="versionComparison" stripe>
        <el-table-column prop="releaseVersion" :label="t('templates.contract.columns.releaseVersion')" />
        <el-table-column prop="explicitVersionUrl" :label="t('templates.contract.columns.generateUrl')" min-width="280">
          <template #default="{ row }">
            <code>{{ row.explicitVersionUrl }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('templates.contract.columns.defaultRouteTarget')" width="160">
          <template #default="{ row }">
            <el-tag v-if="row.isDefaultRouteTarget" type="success" size="small">
              {{ t('templates.contract.defaultRouteTargetYes') }}
            </el-tag>
            <span v-else>{{ t('templates.contract.defaultRouteTargetNo') }}</span>
          </template>
        </el-table-column>
      </el-table>

      <h3>{{ t('templates.contract.sections.policy') }}</h3>
      <dl class="summary-grid">
        <div>
          <dt>{{ t('templates.contract.fields.policyVersion') }}</dt>
          <dd>{{ contract.apiPolicy.policyVersion }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.contract.fields.outputFormats') }}</dt>
          <dd>{{ contract.apiPolicy.allowedOutputFormats.join(', ') }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.contract.fields.outputModes') }}</dt>
          <dd>{{ contract.apiPolicy.allowedOutputModes.join(', ') }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.contract.fields.batchLimits') }}</dt>
          <dd>{{ contract.apiPolicy.batchLimits.syncMaxItems }}</dd>
        </div>
      </dl>

      <h3>{{ t('templates.contract.sections.errorCodes') }}</h3>
      <el-table :data="contract.errorCodes" stripe>
        <el-table-column prop="code" :label="t('templates.contract.columns.errorCode')" width="240" />
        <el-table-column prop="category" :label="t('templates.contract.columns.category')" width="140" />
        <el-table-column prop="message" :label="t('templates.contract.columns.message')" min-width="240" />
        <el-table-column :label="t('templates.contract.columns.retryable')" width="120">
          <template #default="{ row }">
            {{ row.retryable ? t('common.yes') : t('common.no') }}
          </template>
        </el-table-column>
      </el-table>

      <h3>{{ t('templates.contract.sections.examples') }}</h3>
      <ul class="example-list">
        <li v-for="example in contract.examples" :key="example">
          <code>{{ example }}</code>
        </li>
      </ul>
    </template>
  </div>
</template>

<style scoped lang="scss">
.caller-contract-panel {
  h3 {
    margin: 1.5rem 0 0.75rem;
    font-size: 1rem;
  }
}

.panel-description {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.toolbar {
  margin-bottom: 1rem;
}

.environment-select {
  width: 180px;
}

.path-list,
.example-list {
  margin: 0;
  padding-left: 1.25rem;

  li {
    margin-bottom: 0.35rem;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin: 0;

  dt {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0.25rem 0 0;
    font-weight: 500;
  }
}
</style>
