import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [vue()],
  build: {
    // Keep the production bundle inside the Go module so it can be embedded in
    // the API binary. Development still uses Vite's in-memory dev server.
    outDir: fileURLToPath(new URL('../backend/internal/httpapi/webui/dist', import.meta.url)),
    emptyOutDir: true,
  },
  server: {
    // Development stays split into Vite and Go processes while browser API
    // requests remain same-origin from the application's point of view.
    proxy: {
      '/v1': 'http://localhost:8080',
      '/healthz': 'http://localhost:8080',
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
