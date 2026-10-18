import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'
import { webgazerAssets } from './vite/webgazerAssets.ts'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    webgazerAssets(),
  ],
})
