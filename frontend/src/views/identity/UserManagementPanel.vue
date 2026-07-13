<script setup lang="ts">
import UserFormDialog from '@/views/identity/UserFormDialog.vue'
import UserManagementListSection from '@/views/identity/UserManagementListSection.vue'
import UserResetPasswordDialog from '@/views/identity/UserResetPasswordDialog.vue'
import { useUserManagementPanel } from '@/views/identity/useUserManagementPanel'

const {
  identityStore,
  formDialogVisible,
  formDialogMode,
  formDialogRef,
  resetDialogVisible,
  filterGroup,
  filterRole,
  currentPage,
  form,
  canDelete,
  canCreate,
  roleOptions,
  groupOptions,
  isFilterGroupLocked,
  users,
  errorMessageKey,
  reload,
  resetFilters,
  openCreate,
  openEdit,
  submitForm,
  submitResetPassword,
  handleMoreAction,
} = useUserManagementPanel()

defineExpose({
  openCreate,
  submitForm: () => formDialogRef.value?.submitForm(),
  form,
})
</script>

<template>
  <section class="user-panel">
    <UserManagementListSection
      v-model:filter-group="filterGroup"
      v-model:filter-role="filterRole"
      v-model:current-page="currentPage"
      :users="users"
      :loading="identityStore.loadingUsers"
      :error-message-key="errorMessageKey"
      :error-retryable="identityStore.lastUserErrorRetryable"
      :is-filter-group-locked="isFilterGroupLocked"
      :role-options="roleOptions"
      :can-create="canCreate"
      :can-delete="canDelete"
      :page-size="identityStore.userFilters.size ?? 20"
      :total-users="identityStore.usersTotal"
      @reload="reload"
      @reset-filters="resetFilters"
      @create="openCreate"
      @edit="openEdit"
      @more-action="handleMoreAction"
    />

    <UserFormDialog
      ref="formDialogRef"
      v-model:visible="formDialogVisible"
      v-model:form="form"
      :mode="formDialogMode"
      :role-options="roleOptions"
      :group-options="groupOptions"
      :submitting="identityStore.submitting"
      @submit="submitForm"
    />

    <UserResetPasswordDialog
      v-model:visible="resetDialogVisible"
      :submitting="identityStore.submitting"
      @submit="submitResetPassword"
    />
  </section>
</template>

<style scoped lang="scss">
.user-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
</style>
