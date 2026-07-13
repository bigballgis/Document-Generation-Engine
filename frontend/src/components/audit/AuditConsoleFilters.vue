<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'

const eventType = defineModel<string | undefined>('eventType', { required: true })
const requestId = defineModel<string | undefined>('requestId', { required: true })
const eventAtFrom = defineModel<string | undefined>('eventAtFrom', { required: true })
const eventAtTo = defineModel<string | undefined>('eventAtTo', { required: true })
const groupScope = defineModel<string | undefined>('groupScope', { required: true })
const templateId = defineModel<string | undefined>('templateId', { required: true })

defineProps<{
  showGroupFilters: boolean
  isAuditGroupLocked: boolean
  auditEventTypeOptions: Array<{ value: string; label: string }>
  templateOptions: Array<{ value: string; label: string }>
  loadingTemplates: boolean
}>()

const emit = defineEmits<{
  apply: []
  reset: []
  'template-search': [query: string]
}>()

const { t } = useI18n()
</script>

<template>
  <el-card shadow="never" class="filters-card">
    <div class="filters-grid">
      <el-form-item :label="t('audit.filters.eventType')">
        <AppSearchSelect
          v-model="eventType"
          clearable
          :placeholder="t('audit.filters.eventTypePlaceholder')"
        >
          <el-option
            v-for="option in auditEventTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </AppSearchSelect>
      </el-form-item>
      <el-form-item :label="t('audit.filters.requestId')">
        <el-input
          v-model="requestId"
          clearable
          :placeholder="t('audit.filters.requestIdPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('audit.filters.eventAtFrom')">
        <el-date-picker
          v-model="eventAtFrom"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss[Z]"
          :placeholder="t('audit.filters.eventAtFrom')"
          clearable
        />
      </el-form-item>
      <el-form-item :label="t('audit.filters.eventAtTo')">
        <el-date-picker
          v-model="eventAtTo"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss[Z]"
          :placeholder="t('audit.filters.eventAtTo')"
          clearable
        />
      </el-form-item>
      <el-form-item v-if="showGroupFilters" :label="t('audit.filters.groupScope')">
        <ScopedGroupSelect
          v-model="groupScope"
          :clearable="!isAuditGroupLocked"
        />
      </el-form-item>
      <el-form-item v-if="showGroupFilters" :label="t('audit.filters.templateId')">
        <AppSearchSelect
          v-model="templateId"
          clearable
          filterable
          remote
          :remote-method="(query: string) => emit('template-search', query)"
          :loading="loadingTemplates"
          :placeholder="t('audit.filters.templateIdPlaceholder')"
        >
          <el-option
            v-for="option in templateOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </AppSearchSelect>
      </el-form-item>
      <div class="filters-actions">
        <el-button type="primary" @click="emit('apply')">
          {{ t('audit.filters.apply') }}
        </el-button>
        <el-button text @click="emit('reset')">
          {{ t('audit.filters.reset') }}
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.filters-card {
  margin-bottom: var(--space-6);
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-3) var(--space-4);
  align-items: end;
}

.filters-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}
</style>
