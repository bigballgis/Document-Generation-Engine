import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import type { PluginOption } from 'vite'

type ElementPlusStyleMode = 'sass' | false

/** Shared Vite/Vitest plugins for on-demand Element Plus (SOR-P06). */
export function createAppPlugins(options: { elementPlusStyle?: ElementPlusStyleMode } = {}): PluginOption[] {
  const importStyle = options.elementPlusStyle ?? 'sass'
  return [
    vue(),
    AutoImport({
      dts: 'src/auto-imports.d.ts',
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver({ importStyle })],
    }),
    Components({
      dts: false,
      resolvers: [ElementPlusResolver({ importStyle })],
    }),
  ]
}

export { resolveManualChunk } from './build/manualChunks'
