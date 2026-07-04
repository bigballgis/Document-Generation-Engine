/** Rollup manual chunk strategy for vendor splitting (SOR-P06). */
export function resolveManualChunk(id: string): string | undefined {
  if (!id.includes('node_modules')) {
    return undefined
  }
  if (id.includes('@element-plus/icons-vue')) {
    return 'element-icons'
  }
  if (id.includes('element-plus')) {
    return 'element-plus'
  }
  if (
    id.includes('/vue/') ||
    id.includes('/vue-router/') ||
    id.includes('/pinia/') ||
    id.includes('/@vue/')
  ) {
    return 'vue-vendor'
  }
  if (id.includes('axios') || id.includes('vue-i18n')) {
    return 'app-vendor'
  }
  return 'vendor'
}
