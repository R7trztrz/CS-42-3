import { readFileSync, readdirSync, statSync } from 'node:fs'
import { createRequire } from 'node:module'
import path from 'node:path'
import type { Plugin } from 'vite'

/**
 * Serves (dev) and emits (build) the WebGazer runtime from the `webgazer` npm package, so the
 * participant page loads the exact version pinned in package.json without vendoring files.
 *
 * - `webgazer.js` is WebGazer's own webpack build. It installs `window.webgazer` and must be
 *   loaded as a classic script, not bundled: its MediaPipe code does not survive re-bundling.
 * - `mediapipe/face_mesh/*` are the face-mesh model, WASM and loader files WebGazer fetches at
 *   runtime from `params.faceMeshSolutionPath` (~17 MB).
 *
 * Files are published under a versioned directory, `webgazer/<version>/`, exposed to the app as
 * `__WEBGAZER_ASSET_DIR__`, so a version bump never reuses a stale cached file.
 */
const require = createRequire(import.meta.url)
const SKIPPED_FACE_MESH_FILES = new Set(['README.md', 'index.d.ts', 'package.json'])
const CONTENT_TYPES: Record<string, string> = {
  '.js': 'text/javascript',
  '.wasm': 'application/wasm',
  '.data': 'application/octet-stream',
  '.binarypb': 'application/octet-stream',
}

interface WebGazerFile {
  /** Path relative to the asset directory, using forward slashes. */
  readonly publicPath: string
  readonly sourcePath: string
}

function webgazerPackage(): { version: string; distDir: string } {
  const packageJsonPath = require.resolve('webgazer/package.json')
  const { version } = JSON.parse(readFileSync(packageJsonPath, 'utf8')) as { version: string }
  return { version, distDir: path.join(path.dirname(packageJsonPath), 'dist') }
}

function runtimeFiles(distDir: string): WebGazerFile[] {
  const faceMeshDir = path.join(distDir, 'mediapipe', 'face_mesh')
  const faceMesh = readdirSync(faceMeshDir)
    .filter((name) => !SKIPPED_FACE_MESH_FILES.has(name))
    .filter((name) => statSync(path.join(faceMeshDir, name)).isFile())
    .sort()
    .map((name) => ({
      publicPath: `mediapipe/face_mesh/${name}`,
      sourcePath: path.join(faceMeshDir, name),
    }))
  return [{ publicPath: 'webgazer.js', sourcePath: path.join(distDir, 'webgazer.js') }, ...faceMesh]
}

export function webgazerAssets(): Plugin {
  const { version, distDir } = webgazerPackage()
  const assetDir = `webgazer/${version}/`
  const files = runtimeFiles(distDir)
  const byPublicPath = new Map(files.map((file) => [file.publicPath, file]))

  return {
    name: 'webgazer-assets',
    config() {
      return { define: { __WEBGAZER_ASSET_DIR__: JSON.stringify(assetDir) } }
    },
    configureServer(server) {
      const prefix = `${server.config.base}${assetDir}`
      server.middlewares.use((request, response, next) => {
        const url = request.url?.split('?')[0] ?? ''
        if (!url.startsWith(prefix)) {
          next()
          return
        }
        const file = byPublicPath.get(decodeURIComponent(url.slice(prefix.length)))
        if (!file) {
          response.statusCode = 404
          response.end()
          return
        }
        response.setHeader(
          'Content-Type',
          CONTENT_TYPES[path.extname(file.sourcePath)] ?? 'application/octet-stream',
        )
        response.setHeader('Cache-Control', 'no-cache')
        response.end(readFileSync(file.sourcePath))
      })
    },
    generateBundle() {
      for (const file of files) {
        this.emitFile({
          type: 'asset',
          fileName: `${assetDir}${file.publicPath}`,
          source: readFileSync(file.sourcePath),
        })
      }
    },
  }
}
