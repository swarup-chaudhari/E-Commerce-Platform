import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev, proxy API calls to the gateway so the browser sees one origin (no CORS needed).
const gateway = process.env.GATEWAY_URL || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': gateway,
      '/auth': gateway,
    },
  },
});
