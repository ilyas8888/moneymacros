import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    // `@` points at src/ so shadcn/ui components resolve `@/lib/utils` etc.
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // Forward /api calls to the Spring Boot backend during development, so the
    // browser talks to a single origin and there is no CORS to configure.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
