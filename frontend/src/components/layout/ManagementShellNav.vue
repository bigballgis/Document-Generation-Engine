<script setup lang="ts">
import { type Component } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import type { NavGroupDefinition, NavItemDefinition } from '@/navigation/navStructure'

defineProps<{
  collapsed: boolean
  navGroups: NavGroupDefinition[]
  navigationAriaLabel: string
  expandLabel: string
  collapseLabel: string
  getNavIcon: (itemId: string) => Component | undefined
  isNavItemActive: (item: NavItemDefinition) => boolean
  resolveItemLabel: (labelKey: string) => string
}>()

const emit = defineEmits<{
  navigate: [item: NavItemDefinition]
  'toggle-collapsed': []
}>()
</script>

<template>
  <aside class="shell-nav" :class="{ 'shell-nav--collapsed': collapsed }">
    <nav :aria-label="navigationAriaLabel">
      <section
        v-for="group in navGroups"
        :key="group.id"
        class="nav-group"
      >
        <h2 v-if="!collapsed" class="nav-group-label">{{ resolveItemLabel(group.labelKey) }}</h2>
        <template v-for="item in group.items" :key="item.id">
          <el-tooltip
            v-if="collapsed"
            :content="resolveItemLabel(item.labelKey)"
            placement="right"
            :show-after="200"
          >
            <button
              type="button"
              class="nav-item nav-item--icon-only"
              :class="{ active: isNavItemActive(item) }"
              :aria-label="resolveItemLabel(item.labelKey)"
              @click="emit('navigate', item)"
            >
              <el-icon v-if="getNavIcon(item.id)">
                <component :is="getNavIcon(item.id)" />
              </el-icon>
            </button>
          </el-tooltip>
          <button
            v-else
            type="button"
            class="nav-item"
            :class="{ active: isNavItemActive(item) }"
            @click="emit('navigate', item)"
          >
            <el-icon v-if="getNavIcon(item.id)" class="nav-item__icon">
              <component :is="getNavIcon(item.id)" />
            </el-icon>
            <span class="nav-item__label">{{ resolveItemLabel(item.labelKey) }}</span>
          </button>
        </template>
      </section>
    </nav>

    <div class="nav-collapse-toggle">
      <el-tooltip
        :content="collapsed ? expandLabel : collapseLabel"
        placement="right"
      >
        <button
          type="button"
          class="collapse-btn"
          :aria-label="collapsed ? expandLabel : collapseLabel"
          @click="emit('toggle-collapsed')"
        >
          <el-icon>
            <ArrowLeft v-if="!collapsed" />
            <ArrowRight v-else />
          </el-icon>
        </button>
      </el-tooltip>
    </div>
  </aside>
</template>

<style scoped lang="scss" src="./ManagementShellNav.scss"></style>
