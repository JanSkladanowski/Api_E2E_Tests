import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';

const applicationStaticDirectory = fileURLToPath(
  new URL('../application/src/main/resources/static', import.meta.url),
);

export default defineConfig({
  server: {
    host: 'localhost',
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: applicationStaticDirectory,
    emptyOutDir: true,
    rollupOptions: {
      input: {
        home: fileURLToPath(new URL('./index.html', import.meta.url)),
        issues: fileURLToPath(new URL('./issues.html', import.meta.url)),
      },
    },
  },
});
