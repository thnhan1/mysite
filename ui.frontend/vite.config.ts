import { defineConfig } from 'vite';
import path from 'path';
import { fileURLToPath } from 'url';
import vue from '@vitejs/plugin-vue';
import tsconfigPaths from 'vite-tsconfig-paths';
import { viteForAem } from '@aem-vite/vite-aem-plugin';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// ============================================================
// Path constants — sửa 1 lần ở đây cho mọi reference
// ============================================================
const SRC_DIR = 'src/main/webpack';  // TODO: rename folder → 'src/main/vite'
const SITE_DIR_NAME = 'site';
const CLIENTLIB_DIR = 'clientlib-site';
const ASSETS_DIR = 'assets';

const SITE_PATH = path.resolve(__dirname, SRC_DIR, SITE_DIR_NAME);
const ENTRY_TS = `${SRC_DIR}/${SITE_DIR_NAME}/main.ts`;
const VARIABLES_PATH = path.resolve(SITE_PATH, '_variables').replace(/\\/g, '/');

export default defineConfig(({ command, mode }) => ({
  base: command === 'build'
    ? '/etc.clientlibs/mysite/clientlibs/'
    : '/',
  publicDir: command === 'build' ? false : `${SRC_DIR}/${ASSETS_DIR}`,

  build: {
    reportCompressedSize: false,
    manifest: false,
    minify: mode === 'development' ? false : 'terser',
    outDir: 'dist',
    sourcemap: command === 'serve' ? 'inline' : false,
    cssMinify: mode !== 'development',
    rollupOptions: {
      input: {
        site: ENTRY_TS,
      },
      output: {
        entryFileNames: `${CLIENTLIB_DIR}/[name].[hash].js`,
        chunkFileNames: `${CLIENTLIB_DIR}/resources/chunks/[name].[hash].js`,
        assetFileNames: (assetInfo) => {
          if (assetInfo.name?.endsWith('.css')) {
            return `${CLIENTLIB_DIR}/[name].[hash][extname]`;
          }
          return `${CLIENTLIB_DIR}/resources/[name].[hash][extname]`;
        },
      },
    },
  },

  css: {
    preprocessorOptions: {
      scss: {
        includePaths: [SITE_PATH],
        additionalData: `@import '${VARIABLES_PATH}';\n`,
        silenceDeprecations: ['global-builtin', 'import', 'color-functions'],
      },
    },
  },

  plugins: [
    vue(),
    tsconfigPaths(),
    viteForAem({
      contentPaths: ['mysite/en'],
      publicPath: '/etc.clientlibs/mysite/clientlibs/clientlib-site',
      aem: {
        host: 'localhost',
        port: 4502,
      },
    }),
  ],

  server: {
    origin: 'http://localhost:3000',
    port: 3000,
    strictPort: false,
  },
}));
