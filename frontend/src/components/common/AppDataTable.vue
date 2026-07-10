<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, onUpdated, ref, useAttrs, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { shouldActivateFromKeyboard } from '@/composables/useActivatableTableRow'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<{
    activatable?: boolean
    /** Optional per-row accessible name; defaults to common.activatableRowOpen */
    rowAriaLabel?: (row: unknown) => string
  }>(),
  {
    activatable: false,
  },
)

const attrs = useAttrs()
const { t } = useI18n()
const tableRef = ref<{
  $el?: HTMLElement
  store?: { states?: { data?: { value?: unknown[] } } }
} | null>(null)

const ACTIVATABLE_ROW_SELECTOR = 'tbody tr.app-data-table__activatable-row'

const rowClassName = computed(() => {
  const external = attrs['row-class-name']
  if (typeof external === 'function') {
    return external
  }
  if (props.activatable) {
    return () => 'app-data-table__activatable-row'
  }
  return external
})

function tableRootEl(): HTMLElement | null {
  return tableRef.value?.$el ?? null
}

/**
 * Row list that matches the visible tbody order (EP client sort/filter).
 * Must not use parent :data alone — EP reorders display without rewriting attrs.data.
 */
function displayRowData(): unknown[] | undefined {
  const storeData = tableRef.value?.store?.states?.data?.value
  if (Array.isArray(storeData)) {
    return storeData
  }
  const data = attrs.data
  return Array.isArray(data) ? data : undefined
}

function resolveRowData(tr: HTMLElement): unknown | undefined {
  const tbody = tr.parentElement
  if (!tbody) {
    return undefined
  }
  const rows = Array.from(tbody.querySelectorAll(ACTIVATABLE_ROW_SELECTOR))
  const index = rows.indexOf(tr)
  if (index < 0) {
    return undefined
  }
  const data = displayRowData()
  if (!data) {
    return undefined
  }
  return data[index]
}

function invokeRowClick(row: unknown, event: Event) {
  const handler = attrs.onRowClick ?? attrs['onRow-click']
  if (typeof handler === 'function') {
    ;(handler as (row: unknown, column: unknown, event: Event) => void)(row, undefined, event)
  }
}

function enhanceActivatableRows() {
  if (!props.activatable) {
    return
  }
  const root = tableRootEl()
  if (!root) {
    return
  }
  const rows = root.querySelectorAll(ACTIVATABLE_ROW_SELECTOR)
  rows.forEach((node) => {
    const tr = node as HTMLElement
    tr.tabIndex = 0
    const row = resolveRowData(tr)
    const label =
      row !== undefined && props.rowAriaLabel
        ? props.rowAriaLabel(row)
        : t('common.activatableRowOpen')
    tr.setAttribute('aria-label', label)
  })
}

function onTableKeydown(event: KeyboardEvent) {
  if (!props.activatable) {
    return
  }
  if (!shouldActivateFromKeyboard(event)) {
    return
  }
  const target = event.target
  if (!(target instanceof HTMLElement)) {
    return
  }
  if (!target.classList.contains('app-data-table__activatable-row')) {
    return
  }
  event.preventDefault()
  const row = resolveRowData(target)
  if (row !== undefined) {
    invokeRowClick(row, event)
  }
}

let enhanceScheduled = false
function scheduleEnhance() {
  if (!props.activatable || enhanceScheduled) {
    return
  }
  enhanceScheduled = true
  void nextTick(() => {
    enhanceScheduled = false
    enhanceActivatableRows()
  })
}

onMounted(scheduleEnhance)
onUpdated(scheduleEnhance)

watch(
  () => attrs.data,
  () => {
    scheduleEnhance()
  },
  { deep: true },
)

// EP client sort/filter mutates store display data without changing parent :data.
watch(
  () => tableRef.value?.store?.states?.data?.value,
  () => {
    scheduleEnhance()
  },
  { deep: true },
)

watch(
  () => props.activatable,
  () => {
    scheduleEnhance()
  },
)

onBeforeUnmount(() => {
  enhanceScheduled = false
})
</script>

<template>
  <el-table
    ref="tableRef"
    class="app-data-table"
    :class="{ 'app-data-table--activatable': activatable }"
    border
    size="default"
    show-overflow-tooltip
    :row-class-name="rowClassName"
    v-bind="attrs"
    @keydown="onTableKeydown"
  >
    <slot />
    <template v-if="$slots.empty" #empty>
      <slot name="empty" />
    </template>
  </el-table>
</template>

<style scoped lang="scss">
.app-data-table {
  width: 100%;

  :deep(.el-table__header-wrapper) {
    th.el-table__cell {
      vertical-align: top;
      background: var(--surface-muted);

      .cell {
        position: relative;
        display: block;
        line-height: 1.25;
        padding-top: 0.5rem;
        padding-bottom: 0.5rem;
        white-space: normal;
        word-break: normal;
      }

      .caret-wrapper {
        position: absolute;
        top: 0.55rem;
        right: 0.25rem;
      }
    }

    th.el-table__cell:focus-visible {
      outline: var(--focus-ring-width) solid var(--focus-ring-color);
      outline-offset: var(--focus-ring-offset);
    }
  }

  :deep(.el-table__body-wrapper) {
    td.el-table__cell .cell {
      line-height: 1.45;
    }
  }
}

.app-data-table--activatable {
  :deep(.app-data-table__activatable-row) {
    cursor: pointer;
  }

  :deep(.app-data-table__activatable-row:focus-visible) {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: calc(-1 * var(--focus-ring-offset));
  }
}
</style>
