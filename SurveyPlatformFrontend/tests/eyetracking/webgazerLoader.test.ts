// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  defaultWebGazerAssets,
  loadWebGazer,
  type LoadedWebGazer,
} from '../../src/eyetracking/participant'
import { resetWebGazerLoaderForTests } from '../../src/eyetracking/participant/webgazerLoader'

const assets = {
  scriptUrl: '/webgazer/9.9.9/webgazer.js',
  faceMeshSolutionPath: '/webgazer/9.9.9/mediapipe/face_mesh',
}

let appended: HTMLScriptElement[] = []

beforeEach(() => {
  resetWebGazerLoaderForTests()
  delete window.webgazer
  appended = []
  // Capture the script instead of attaching it, so nothing is fetched.
  vi.spyOn(document.head, 'appendChild').mockImplementation((node) => {
    appended.push(node as HTMLScriptElement)
    return node
  })
})

afterEach(() => {
  delete window.webgazer
})

describe('loadWebGazer', () => {
  it('injects one script, then configures the absolute face-mesh path on the global', async () => {
    const first = loadWebGazer({ assets })
    const second = loadWebGazer({ assets })
    expect(second).toBe(first)
    expect(appended).toHaveLength(1)
    expect(appended[0].getAttribute('src')).toBe(assets.scriptUrl)

    const webgazer = { params: { faceMeshSolutionPath: './mediapipe/face_mesh' } }
    window.webgazer = webgazer as unknown as LoadedWebGazer
    appended[0].dispatchEvent(new Event('load'))

    await expect(first).resolves.toBe(webgazer)
    expect(webgazer.params.faceMeshSolutionPath).toBe(assets.faceMeshSolutionPath)
  })

  it('reuses a WebGazer global that is already on the page', async () => {
    const webgazer = { params: {} } as unknown as LoadedWebGazer
    window.webgazer = webgazer

    await expect(loadWebGazer({ assets })).resolves.toBe(webgazer)
    expect(appended).toHaveLength(0)
  })

  it('forgets a failed load so the next call tries again', async () => {
    const failed = loadWebGazer({ assets })
    appended[0].dispatchEvent(new Event('error'))
    await expect(failed).rejects.toThrow(assets.scriptUrl)

    const retry = loadWebGazer({ assets })
    expect(retry).not.toBe(failed)
    expect(appended).toHaveLength(2)
  })

  it('rejects a script that never installs the global, and one that does not load in time', async () => {
    const empty = loadWebGazer({ assets })
    appended[0].dispatchEvent(new Event('load'))
    await expect(empty).rejects.toThrow('did not install window.webgazer')

    vi.useFakeTimers()
    try {
      const slow = loadWebGazer({ assets, timeoutMs: 1000 })
      const settled = expect(slow).rejects.toThrow('within 1000 ms')
      await vi.advanceTimersByTimeAsync(1000)
      await settled
    } finally {
      vi.useRealTimers()
    }
  })

  it('places the assets under the app base URL in the versioned directory', () => {
    expect(defaultWebGazerAssets('/')).toEqual({
      scriptUrl: '/webgazer/0.0.0-test/webgazer.js',
      faceMeshSolutionPath: '/webgazer/0.0.0-test/mediapipe/face_mesh',
    })
    expect(defaultWebGazerAssets('/app').scriptUrl).toBe('/app/webgazer/0.0.0-test/webgazer.js')
  })
})
