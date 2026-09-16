import { CalibrationController, WebGazerGazeEngine } from '../../src/eyetracking'
import type {
  CalibrationAttempt,
  GazeSample,
  WebGazerLike,
  WebGazerPrediction,
} from '../../src/eyetracking'

/** A WebGazer contract double using real canvas MediaStreamTracks, never getUserMedia. */
class BrowserWebGazer implements WebGazerLike {
  callback: ((sample: WebGazerPrediction | null, time: number) => void) | null = null
  streams: MediaStream[] = []
  begins = 0
  training = 0
  resets = 0
  mouseEnabled = false
  gate: Promise<void> = Promise.resolve()
  modelGate: Promise<void> = Promise.resolve()
  fail: string | null = null
  saveDataAcrossSessions() {}
  setRegression() {}
  getRegression() {
    return [
      {
        init: () => {
          this.resets += 1
        },
      },
    ]
  }
  setGazeListener(callback: (sample: WebGazerPrediction | null, time: number) => void) {
    this.callback = callback
  }
  clearGazeListener() {
    this.callback = null
  }
  removeMouseEventListeners() {
    this.mouseEnabled = false
  }
  recordScreenPosition() {
    this.training += 1
  }
  async begin() {
    this.begins += 1
    await this.gate
    if (this.fail) throw new DOMException('Test camera failure', this.fail)
    const tracks = [0, 1].flatMap(() => {
      const canvas = document.createElement('canvas')
      canvas.width = 16
      canvas.height = 16
      canvas.getContext('2d')!.fillRect(0, 0, 16, 16)
      return canvas.captureStream(0).getTracks()
    })
    const stream = new MediaStream(tracks)
    this.streams.push(stream)
    const container = document.createElement('div')
    container.id = 'webgazerVideoContainer'
    const video = document.createElement('video')
    video.id = 'webgazerVideoFeed'
    video.srcObject = stream
    container.appendChild(video)
    document.body.appendChild(container)
    this.mouseEnabled = true
    await this.modelGate
  }
  stopVideo() {
    this.streams.at(-1)?.getTracks()[0].stop()
  }
  // Matches the relevant WebGazer 3.5.3 contract: end removes DOM, but does not stop tracks.
  end() {
    document.getElementById('webgazerVideoContainer')?.remove()
  }
}

const webgazer = new BrowserWebGazer()
let clock = 0
let controller: CalibrationController
let attempt: CalibrationAttempt | null = null
const records: CalibrationAttempt[] = []
const gaze: (GazeSample & { quality: string; sessionId: string })[] = []
let releaseStart: () => void = () => {}
let releaseModel: () => void = () => {}
let trackingEngine: ReturnType<CalibrationController['handoffToTracking']> | null = null
const flush = async () => {
  for (let i = 0; i < 15; i += 1) await Promise.resolve()
}

const harness = {
  async start(options: { delayed?: boolean; hangingModel?: boolean; denied?: boolean } = {}) {
    if (options.delayed)
      webgazer.gate = new Promise((resolve) => {
        releaseStart = resolve
      })
    if (options.hangingModel)
      webgazer.modelGate = new Promise((resolve) => {
        releaseModel = resolve
      })
    if (options.denied) webgazer.fail = 'NotAllowedError'
    const engine = new WebGazerGazeEngine({
      webgazer,
      now: () => clock,
      navigator: {
        mediaDevices: {
          enumerateDevices: async () => [],
          getUserMedia: async () => {
            throw new Error('Browser tests must never request a hardware camera.')
          },
        },
      },
    })
    controller = new CalibrationController({
      engine,
      session: { sessionId: 'browser-session', consentGiven: true, eyeTrackingEnabled: true },
      now: () => clock,
      readViewport: () => ({ width: innerWidth, height: innerHeight, devicePixelRatio }),
      targetSettleMs: 0,
      sink: {
        record: (result) => {
          records.push(result)
        },
      },
    })
    void controller.start().then((result) => {
      attempt = result
    })
    await flush()
    if (options.delayed && webgazer.begins !== 1) {
      throw new Error('The delayed camera test must reach WebGazer.begin before cancellation.')
    }
    if (!options.delayed && !options.hangingModel && controller.phase === 'STARTING') {
      throw new Error('The immediate camera double did not finish starting.')
    }
  },
  async click() {
    const target = controller.currentTarget!
    clock += 20
    webgazer.callback?.({ x: target.targetX + 10, y: target.targetY }, 0)
    controller.registerClick()
    await flush()
  },
  async complete() {
    while (controller.running) await this.click()
  },
  async skip() {
    controller.skip()
    await flush()
  },
  async dispose() {
    controller.dispose()
    await flush()
  },
  async releaseStart() {
    releaseStart()
    await flush()
  },
  async releaseModel() {
    releaseModel()
    await flush()
  },
  handoff() {
    trackingEngine = controller.handoffToTracking()
    trackingEngine.setSampleListener((sample) => {
      gaze.push({ ...sample, quality: attempt!.quality, sessionId: attempt!.sessionId })
    })
    controller.dispose() // Simulates the calibration page unmounting after the handoff.
  },
  emit() {
    clock += 20
    webgazer.callback?.({ x: 300, y: 400 }, 0)
  },
  endSession() {
    trackingEngine?.stop()
  },
  state() {
    return {
      phase: controller.phase,
      attempt,
      records,
      gaze,
      begins: webgazer.begins,
      training: webgazer.training,
      resets: webgazer.resets,
      mouseEnabled: webgazer.mouseEnabled,
      tracks: webgazer.streams.flatMap((stream) =>
        stream.getTracks().map((track) => track.readyState),
      ),
      videoPresent: !!document.getElementById('webgazerVideoFeed'),
    }
  },
}

declare global {
  interface Window {
    fr49Harness: typeof harness
  }
}
window.fr49Harness = harness
