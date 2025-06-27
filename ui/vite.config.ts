import react from '@vitejs/plugin-react-swc';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/pmw-ui',
    build: {
        outDir: './dist/static/pmw-ui',
        emptyOutDir: true
    },
    resolve: {
        alias: [
            { find: '@', replacement: '/src' }
        ]
    },
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
                ws: true,
            },
            '/pmw-api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
                ws: true,
            }
        }
    }
})
