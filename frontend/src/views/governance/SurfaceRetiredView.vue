<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

const props = defineProps<{
  surface: 'document-brands' | 'legal-entities'
}>()

const { t } = useI18n()
const router = useRouter()
const sessionStore = useSessionStore()

const titleKey = computed(() =>
  props.surface === 'document-brands'
    ? 'retiredSurface.documentBrands.title'
    : 'retiredSurface.legalEntities.title',
)

const messageKey = computed(() =>
  props.surface === 'document-brands'
    ? 'retiredSurface.documentBrands.message'
    : 'retiredSurface.legalEntities.message',
)

function goHome() {
  if (sessionStore.authenticated) {
    router.push(sessionStore.defaultHomePath())
    return
  }
  router.push('/login')
}
</script>

<template>
  <div class="surface-retired-page" data-testid="surface-retired-view">
    <el-result icon="info" :title="t(titleKey)" :sub-title="t(messageKey)">
      <template #extra>
        <div class="surface-retired-actions">
          <RouterLink
            class="surface-retired-link"
            data-testid="surface-retired-letterhead-link"
            :to="ROUTE_PATH_BY_KEY[ROUTE_KEYS.masterManagement]"
          >
            {{ t('retiredSurface.actions.openLetterhead') }}
          </RouterLink>
          <RouterLink
            v-if="surface === 'legal-entities'"
            class="surface-retired-link"
            data-testid="surface-retired-legal-holds-link"
            :to="ROUTE_PATH_BY_KEY[ROUTE_KEYS.legalHoldAdministration]"
          >
            {{ t('retiredSurface.actions.openLegalHolds') }}
          </RouterLink>
          <el-button type="primary" data-testid="surface-retired-home" @click="goHome">
            {{ t('retiredSurface.actions.backToHome') }}
          </el-button>
        </div>
      </template>
    </el-result>
  </div>
</template>

<style scoped lang="scss">
.surface-retired-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-bg);
}

.surface-retired-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
}

.surface-retired-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
}
</style>
