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

<style scoped lang="scss">
.shell-nav {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-4) 0 var(--space-2);
  border-right: 1px solid var(--border-default);
  background: var(--surface-card);
  transition: width var(--transition-base);

  &.shell-nav--collapsed {
    width: 64px;
  }
}

nav {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 0 0.75rem;

  .shell-nav--collapsed & {
    padding: 0 0.5rem;
    gap: 0.75rem;
  }
}

.nav-group-label {
  margin: 0 0 var(--space-2) var(--space-4);
  font-size: var(--font-size-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gray-500);
  white-space: nowrap;
  overflow: hidden;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  width: 100%;
  padding: 0.6rem var(--space-4);
  border: none;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  background: transparent;
  text-align: left;
  font: inherit;
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
  transition:
    background-color var(--transition-base),
    color var(--transition-base),
    box-shadow var(--transition-base);

  &:hover {
    background: color-mix(in srgb, var(--brand-primary) 6%, var(--surface-card));
  }

  &.active {
    background: color-mix(in srgb, var(--brand-primary) 10%, var(--surface-card));
    color: var(--brand-primary);
    font-weight: 650;
    box-shadow: inset 3px 0 0 var(--brand-primary);
  }

  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: var(--focus-ring-offset);
  }

  &--icon-only {
    justify-content: center;
    padding: 0.6rem;
    border-radius: var(--radius-sm);
    box-shadow: none;

    &.active {
      box-shadow: none;
      border: 1.5px solid color-mix(in srgb, var(--brand-primary) 40%, transparent);
    }
  }
}

.nav-item__icon {
  flex-shrink: 0;
  font-size: 1rem;
}

.nav-item__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-collapse-toggle {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-2) var(--space-3) var(--space-2);
  border-top: 1px solid var(--border-default);

  .shell-nav--collapsed & {
    justify-content: center;
  }
}

.collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  padding: 0;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    background-color var(--transition-base),
    color var(--transition-base);

  &:hover {
    background: color-mix(in srgb, var(--brand-primary) 6%, var(--surface-card));
    color: var(--brand-primary);
    border-color: color-mix(in srgb, var(--brand-primary) 40%, transparent);
  }

  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: var(--focus-ring-offset);
  }
}
</style>
