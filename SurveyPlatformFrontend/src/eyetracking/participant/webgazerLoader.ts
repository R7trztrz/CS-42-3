import type { WebGazerLike } from '../engine/webgazerGazeEngine'

/** Set by the `webgazer-assets` Vite plugin, e.g. "webgazer/3.5.3/". Absent under Vitest. */
declare const __WEBGAZER_ASSET_DIR__: string | undefined

/** The WebGazer 3.5.3 global, including the params this loader configures. */
export interface LoadedWebGazer extends WebGazerLike {
  params?: { videoElementId?: string; faceMeshSolutionPath?: string }
}

declare global {
  interface Window {
    webgazer?: LoadedWebGazer
  }
}

export interface WebGazerAssets {
  /** URL of WebGazer's own build, loaded as a classic script. */
  readonly scriptUrl: string
  /** Directory WebGazer fetches the MediaPipe face-mesh files from. */
  readonly faceMeshSolutionPath: string
}

export interface LoadWebGazerOptions {
  assets?: WebGazerAssets
  /** Script download budget. The face-mesh model loads later, inside the engine's start. */
  timeoutMs?: number
  document?: Document
}

export const DEFAULT_WEBGAZER_LOAD_TIMEOUT_MS = 60_000

/** Locations published by the Vite plugin under the app's base URL. */
export function defaultWebGazerAssets(baseUrl: string = import.meta.env.BASE_URL): WebGazerAssets {
  const directory = typeof __WEBGAZER_ASSET_DIR__ === 'string' ? __WEBGAZER_ASSET_DIR__ : null
  if (!directory) {
    throw new Error('WebGazer assets are not configured; is the webgazer-assets Vite plugin enabled?')
  }
  const root = `${baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`}${directory}`
  return {
    scriptUrl: `${root}webgazer.js`,
    // WebGazer defaults to "./mediapipe/face_mesh", which resolves against the page URL and so
    // breaks on nested participant routes. An absolute path works from any route.
    faceMeshSolutionPath: `${root}mediapipe/face_mesh`,
  }
}

let pending: Promise<LoadedWebGazer> | null = null

/**
 * Loads WebGazer once per page and returns the global it installs. Nothing is fetched until the
 * participant starts calibration. A failed load is forgotten so a later call can try again.
 */
export function loadWebGazer(options: LoadWebGazerOptions = {}): Promise<LoadedWebGazer> {
  if (pending) return pending
  const attempt = load(options)
  pending = attempt
  attempt.catch(() => {
    if (pending === attempt) pending = null
  })
  return attempt
}

/** Test seam: forget the cached load so each test starts from an empty page. */
export function resetWebGazerLoaderForTests(): void {
  pending = null
}

function load(options: LoadWebGazerOptions): Promise<LoadedWebGazer> {
  return new Promise<LoadedWebGazer>((resolve, reject) => {
    const document = options.document ?? globalThis.document
    const view = document?.defaultView
    if (!document || !view) {
      reject(new Error('WebGazer can only be loaded in a browser document.'))
      return
    }
    let assets: WebGazerAssets
    try {
      assets = options.assets ?? defaultWebGazerAssets()
    } catch (error) {
      reject(error)
      return
    }
    const existing = view.webgazer
    if (existing) {
      resolve(configure(existing, assets))
      return
    }
    const script = document.createElement('script')
    script.src = assets.scriptUrl
    script.async = true
    const timeoutMs = options.timeoutMs ?? DEFAULT_WEBGAZER_LOAD_TIMEOUT_MS
    const fail = (error: Error) => {
      clearTimeout(timer)
      script.remove()
      reject(error)
    }
    const timer = setTimeout(
      () => fail(new Error(`WebGazer did not load within ${timeoutMs} ms.`)),
      timeoutMs,
    )
    script.addEventListener('load', () => {
      clearTimeout(timer)
      const loaded = view.webgazer
      if (loaded) resolve(configure(loaded, assets))
      else fail(new Error('The WebGazer script loaded but did not install window.webgazer.'))
    })
    script.addEventListener('error', () => fail(new Error(`Could not load ${assets.scriptUrl}.`)))
    document.head.appendChild(script)
  })
}

function configure(webgazer: LoadedWebGazer, assets: WebGazerAssets): LoadedWebGazer {
  if (webgazer.params) webgazer.params.faceMeshSolutionPath = assets.faceMeshSolutionPath
  return webgazer
}
