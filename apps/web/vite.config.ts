import { defineConfig, type ProxyOptions } from 'vite'
import react from '@vitejs/plugin-react'

function proxy(target: string): ProxyOptions {
  return {
    target,
    changeOrigin: true,
    configure(server) {
      server.on('proxyReq', (proxyReq) => {
        proxyReq.removeHeader('origin')
      })
    },
  }
}

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': proxy('http://localhost:8080'),
      '/media': proxy('http://localhost:8080'),
    },
  },
})
