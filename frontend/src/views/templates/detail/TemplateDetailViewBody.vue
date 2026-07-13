<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- c is a reactive controller bag owned by the parent shell */
import TemplateDetailLoadedBody from '@/views/templates/detail/TemplateDetailLoadedBody.vue'
import TemplateDetailHeaderActions from '@/views/templates/detail/TemplateDetailHeaderActions.vue'
import TemplateWorkspaceHeader from '@/components/templates/TemplateWorkspaceHeader.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'

/** Parent passes reactive(useTemplateDetailController(...)); refs are auto-unwrapped. */
defineProps<{
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  c: any
}>()
</script>

<template>
  <AppPageLayout>
    <TemplateWorkspaceHeader
      :template-name="c.template?.name ?? c.t('templates.packageHub.loadingTitle')"
      :group-label="c.template ? c.t('templates.detail.groupLabel', { groupCode: c.template.groupCode }) : undefined"
      :status="c.template?.lifecycleStatus"
      :approval-sub-state="c.template?.approvalSubState"
      :back-label="c.isDevEditor ? c.t('templates.releaseDetail.backToHub') : c.t('templates.detail.backToList')"
      @back="c.backToList"
    >
      <template v-if="c.template" #actions>
        <TemplateDetailHeaderActions
          :template-id="c.templateId"
          :external-id="c.template.externalId"
          :show-export-actions="c.showExportActions"
          :show-delete-template-action="c.showDeleteTemplateAction"
          :show-metadata-edit="c.showMetadataEdit"
          :submitting="c.templatesStore.submitting"
          @delete="c.handleDeleteTemplate"
          @edit-metadata="c.metadataEditOpen = true"
        />
      </template>
    </TemplateWorkspaceHeader>

    <LoadErrorPanel
      v-if="c.loadFailed"
      :message-key="c.templatesStore.lastErrorMessageKey ?? 'templates.error.loadDetail'"
      @retry="c.loadTemplate"
    />

    <el-skeleton v-else-if="c.showDetailSkeleton" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!c.template"
      title-key="templates.empty.notFoundTitle"
      description-key="templates.empty.notFoundDescription"
    />

    <TemplateDetailLoadedBody v-else-if="c.template" :c="c" />
  </AppPageLayout>
</template>
