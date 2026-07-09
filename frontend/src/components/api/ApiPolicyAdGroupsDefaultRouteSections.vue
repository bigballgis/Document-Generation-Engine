<script setup lang="ts">
import { inject } from 'vue'
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { API_POLICY_DOMAIN_EDITOR_KEY } from '@/components/api/apiPolicyDomainEditorContext'

defineProps<{
  canEdit: boolean
  submitting: boolean
}>()

const emit = defineEmits<{
  saveAdGroups: []
  saveDefaultRoute: []
}>()

const { t } = useI18n()
const { apiPolicy, forms, allowedAdGroupsText } = inject(API_POLICY_DOMAIN_EDITOR_KEY)!
const { adGroupsForm, defaultRouteForm } = forms
</script>

<template>
  <section id="policy-domain-AD_GROUP_AUTHORIZATION" class="l1-section">
    <h3>{{ t('templates.policy.l1.adGroupsTitle') }}</h3>
    <p class="field-hint">{{ t('templates.policy.l1.adGroupsHint') }}</p>
    <dl class="policy-summary">
      <div>
        <dt>{{ t('templates.policy.allowedAdGroups') }}</dt>
        <dd>
          <el-tooltip
            v-if="allowedAdGroupsText"
            :content="allowedAdGroupsText"
            placement="top"
          >
            <span class="policy-value policy-value--truncate policy-ad-groups">
              {{ allowedAdGroupsText }}
            </span>
          </el-tooltip>
          <span v-else class="policy-empty">{{ t('apiPolicy.detail.summary.empty') }}</span>
        </dd>
      </div>
    </dl>
    <el-form v-if="canEdit" label-position="top" class="inline-form">
      <el-form-item :label="t('templates.policy.l1.editAdGroups')">
        <AppSearchSelect
          v-model="adGroupsForm.allowedAdGroups"
          multiple
          filterable
          allow-create
          default-first-option
          :placeholder="t('templates.policy.allowedAdGroupsPlaceholder')"
        />
      </el-form-item>
      <div class="action-row">
        <el-button type="primary" :loading="submitting" @click="emit('saveAdGroups')">
          {{ t('templates.policy.l1.saveAdGroups') }}
        </el-button>
      </div>
    </el-form>
  </section>

  <section id="policy-domain-DEFAULT_ROUTE_TARGET" class="l1-section">
    <h3>{{ t('templates.policy.l1.defaultRouteTitle') }}</h3>
    <dl class="policy-summary">
      <div>
        <dt>{{ t('templates.policy.defaultRouteReleaseVersion') }}</dt>
        <dd>
          {{ apiPolicy?.defaultRouteReleaseVersion || t('apiPolicy.detail.summary.empty') }}
        </dd>
      </div>
    </dl>
    <el-form v-if="canEdit" label-position="top" class="inline-form">
      <el-form-item :label="t('templates.policy.l1.editDefaultRoute')">
        <el-input v-model="defaultRouteForm.defaultRouteReleaseVersion" />
      </el-form-item>
      <div class="action-row">
        <el-button type="primary" :loading="submitting" @click="emit('saveDefaultRoute')">
          {{ t('templates.policy.l1.saveDefaultRoute') }}
        </el-button>
      </div>
    </el-form>
  </section>
</template>

<style scoped lang="scss">
@use './apiPolicyDomainEditor.shared.scss';
</style>
