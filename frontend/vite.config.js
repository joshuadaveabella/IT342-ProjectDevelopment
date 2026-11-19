import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The browser talks only to :5173; /api is proxied to Spring Boot so cookies stay same-origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: false } },
  },
});
