<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import ManagementShell from '@/components/layout/ManagementShell.vue'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()
const useShell = computed(() => !route.meta.public)

/** FOS-W5-3: keep Element Plus chrome (pagination, empty, date picker) in sync with app locale. */
const elementLocale = computed(() => (appStore.locale === 'zh-CN' ? zhCn : en))
</script>

<template>
  <el-config-provider :locale="elementLocale">
    <ManagementShell v-if="useShell">
      <RouterView />
    </ManagementShell>
    <RouterView v-else />
  </el-config-provider>
</template>
