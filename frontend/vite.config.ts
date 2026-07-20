import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

import { bypassSpaApiRoutes } from './src/dev/spaApiProxyBypass'
import { createAppPlugins, resolveManualChunk } from './vite.shared'

export default defineConfig({
  plugins: createAppPlugins(),
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: resolveManualChunk,
      },
    },
  },
  server: {
    host: '127.0.0.1',
    port: Number(process.env.FRONTEND_PORT ?? 5173),
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
        // SPA hard-refresh for /api/policies and /api/packages (see nginx.conf).
        bypass(req) {
          return bypassSpaApiRoutes(req.url)
        },
      },
    },
  },
})
