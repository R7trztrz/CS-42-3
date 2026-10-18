import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { expect, test, type Page } from '@playwright/test'

/**
 * The participant page end to end, with the real WebGazer build served by the webgazer-assets
 * plugin and Chromium's synthetic camera. The backend is stubbed at the same origin (see
 * VITE_API_BASE_URL in playwright.config.ts) and records what the page sends.
 *
 * The synthetic camera shows no face, so WebGazer produces no predictions: calibration finishes
 * unmeasured and browsing yields no gaze batches. Gaze content is covered by the unit, wire
 * contract and backend integration tests; this test covers loading, the camera lifecycle and the
 * participant flow.
 */
const API = '/stub-api'
const STUDY_TOKEN = 'e2e-study-token'
const SESSION_ID = '44444444-4444-4444-8444-444444444444'
const SESSION_TOKEN = 'e'.repeat(43)
const NOW = '2026-10-05T00:00:00Z'
const feed = JSON.parse(
  readFileSync(fileURLToPath(new URL('../../template-json/facebook-template.json', import.meta.url)), 'utf8'),
) as Record<string, unknown>

test.use({
  permissions: ['camera'],
  launchOptions: {
    ...(process.env.EYETRACKING_BROWSER_EXECUTABLE
      ? { executablePath: process.env.EYETRACKING_BROWSER_EXECUTABLE }
      : {}),
    args: ['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream'],
  },
})

interface Backend {
  phase: string
  status: string
  calls: string[]
  tokens: Set<string | undefined>
  attempts: Record<string, unknown>[]
  completions: Record<string, unknown>[]
  batches: Record<string, unknown>[]
}

async function stubBackend(page: Page): Promise<Backend> {
  const backend: Backend = {
    phase: 'CONSENT',
    status: 'IN_PROGRESS',
    calls: [],
    tokens: new Set(),
    attempts: [],
    completions: [],
    batches: [],
  }
  const consentDocument = {
    version: 'platform-default-v1',
    title: 'Informed consent',
    content: 'E2E consent text.',
    approvedForProduction: false,
  }
  const session = () => ({
    sessionId: SESSION_ID,
    status: backend.status,
    phase: backend.phase,
    abandonmentReason: null,
    currentQuestionItemId: null,
    questionnaireReadyToSubmit: false,
    studyAvailable: true,
    studyTitle: 'E2E study',
    studyDescription: null,
    eyeTrackingEnabled: true,
    questionnaireEnabled: false,
    consentDocument,
    enteredAt: NOW,
    consentedAt: backend.phase === 'CONSENT' ? null : NOW,
    calibrationCompletedAt: backend.completions.length > 0 ? NOW : null,
    browsingCompletedAt: null,
    completedAt: backend.status === 'COMPLETED' ? NOW : null,
    abandonedAt: null,
    lastActivityAt: NOW,
  })

  // The template's images point at external hosts; keep the test off the network.
  await page.route(
    (url) => url.hostname !== 'localhost' && url.hostname !== '127.0.0.1',
    (route) => route.abort(),
  )
  await page.route(`**${API}/**`, async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname.slice(API.length)
    const call = `${request.method()} ${path}`
    backend.calls.push(call)
    const reply = (status: number, body: unknown) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })

    if (call === `GET /api/participation/${STUDY_TOKEN}`) {
      return reply(200, {
        title: 'E2E study',
        description: null,
        eyeTrackingEnabled: true,
        questionnaireEnabled: false,
        theme: 'facebook',
        content: feed,
      })
    }
    if (call === `POST /api/participation/${STUDY_TOKEN}/sessions`) {
      return reply(201, { ...session(), sessionToken: SESSION_TOKEN, studyDescription: null })
    }
    backend.tokens.add(request.headers()['x-participant-session-token'])
    if (call === 'GET /api/participant-session') return reply(200, session())
    if (call === 'PUT /api/participant-session/consent') {
      backend.phase = 'CALIBRATION'
      return reply(200, session())
    }
    if (call.startsWith('PUT /api/participant-session/calibration-attempts/')) {
      backend.attempts.push(request.postDataJSON())
      return reply(201, {})
    }
    if (call === 'POST /api/participant-session/calibration-completion') {
      backend.completions.push(request.postDataJSON())
      backend.phase = 'BROWSING'
      return reply(200, session())
    }
    if (call === 'POST /api/participant-session/gaze-batches') {
      backend.batches.push(request.postDataJSON())
      return reply(201, {})
    }
    if (call === 'POST /api/participant-session/browsing-completion') {
      backend.phase = 'FINISHED'
      backend.status = 'COMPLETED'
      return reply(200, session())
    }
    return reply(404, { code: 'NOT_FOUND', error: call })
  })
  return backend
}

test('calibrates with the real WebGazer build, browses, and releases the camera on leaving', async ({
  page,
}) => {
  test.setTimeout(180_000)
  const backend = await stubBackend(page)
  // localhost, not 127.0.0.1: WebGazer alerts on plain-http hosts other than localhost.
  await page.goto(`http://localhost:4186/participate/${STUDY_TOKEN}`)

  await page.getByRole('button', { name: 'Begin study' }).click()
  await page.getByLabel('I have read the participant information and agree to take part.').check()
  await page.getByRole('button', { name: 'I agree to participate' }).click()
  await page.getByRole('button', { name: 'Start calibration' }).click()

  // The first target appears only after the script, the camera and the MediaPipe face mesh
  // (WASM and model files from the versioned asset directory) have all loaded.
  const dot = page.getByRole('button', { name: 'Calibration point' })
  await expect(dot).toBeVisible({ timeout: 120_000 })
  const calibrating = await page.evaluate(() => {
    const video = document.getElementById('webgazerVideoFeed') as HTMLVideoElement | null
    const container = document.getElementById('webgazerVideoContainer')
    return {
      tracks: ((video?.srcObject as MediaStream | null)?.getTracks() ?? []).map((t) => t.readyState),
      preview: container ? getComputedStyle(container).display : null,
      faceMeshPath: (window as unknown as { webgazer?: { params?: { faceMeshSolutionPath?: string } } })
        .webgazer?.params?.faceMeshSolutionPath,
      gazeDot: (window as unknown as { webgazer?: { params?: { showGazeDot?: boolean } } }).webgazer
        ?.params?.showGazeDot,
      // The calibration layer must not create a stacking context, or the preview covers targets.
      layer: (() => {
        const style = getComputedStyle(document.querySelector('[role="dialog"]') as Element)
        return { position: style.position, zIndex: style.zIndex }
      })(),
    }
  })
  expect(calibrating.tracks).toEqual(['live'])
  expect(calibrating.preview).not.toBe('none')
  expect(calibrating.faceMeshPath).toMatch(/^\/webgazer\/3\.5\.3\/mediapipe\/face_mesh$/)
  expect(calibrating.gazeDot).toBe(true)
  expect(calibrating.layer).toEqual({ position: 'static', zIndex: 'auto' })

  const overlaps: string[] = []
  for (let click = 0; click < 45; click += 1) {
    await expect(dot).toBeEnabled()
    if (click % 5 === 0) {
      const target = await dot.boundingBox()
      const preview = await page.locator('#webgazerVideoContainer').boundingBox()
      if (
        target &&
        preview &&
        target.x < preview.x + preview.width &&
        preview.x < target.x + target.width &&
        target.y < preview.y + preview.height &&
        preview.y < target.y + target.height
      )
        overlaps.push(`${target.x},${target.y}`)
    }
    // WebGazer runs face-mesh inference on the main thread every frame, which makes Playwright's
    // wait-for-stable-frames slow in a software-rendered headless browser; the dot never moves
    // while it is enabled, so the stability wait is skipped.
    await dot.click({ force: true })
  }
  await expect(dot).toHaveCount(0)
  expect(overlaps).toEqual([])
  await expect(page.getByText('Its accuracy could not be measured.')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Recalibrate' })).toBeVisible()
  const video = await page.evaluateHandle(() => document.getElementById('webgazerVideoFeed'))
  // The camera stays on for browsing; WebGazer's preview and dot are for the targets only.
  const cameraState = () =>
    page.evaluate(() => {
      const container = document.getElementById('webgazerVideoContainer')
      const feed = document.getElementById('webgazerVideoFeed') as HTMLVideoElement | null
      return {
        tracks: ((feed?.srcObject as MediaStream | null)?.getTracks() ?? []).map((t) => t.readyState),
        preview: container ? getComputedStyle(container).display : null,
        gazeDot: (window as unknown as { webgazer?: { params?: { showGazeDot?: boolean } } }).webgazer
          ?.params?.showGazeDot,
      }
    })
  expect(await cameraState()).toEqual({ tracks: ['live'], preview: 'none', gazeDot: false })

  await page.getByRole('button', { name: 'Continue', exact: true }).click()
  // The set-up review that follows says the camera is still on.
  await expect(page.getByText('Calibration confirmed')).toBeVisible()
  await expect(page.getByText('Your camera stays on until you finish browsing')).toBeVisible()
  expect(await cameraState()).toEqual({ tracks: ['live'], preview: 'none', gazeDot: false })
  await page.getByRole('button', { name: 'Continue to activity' }).click()
  await page.getByRole('button', { name: 'Start activity' }).click()

  const leave = page.getByRole('button', { name: 'Finish browsing' })
  await expect(leave).toBeVisible()
  await expect(page.locator('[data-component-id]').first()).toBeAttached()
  const browsing = await page.evaluate(() => {
    const container = document.getElementById('webgazerVideoContainer')
    return {
      preview: container ? getComputedStyle(container).display : null,
      // What FR-50 attribution reads under a point in the middle of the feed.
      widgets: document.querySelectorAll('[data-component-id]').length,
      gazeDot: (window as unknown as { webgazer?: { params?: { showGazeDot?: boolean } } }).webgazer
        ?.params?.showGazeDot,
    }
  })
  expect(browsing.preview).toBe('none')
  expect(browsing.widgets).toBeGreaterThan(0)
  // Without VITE_EYE_TRACKING_TEST_MODE participants see no gaze dot and no switch for it.
  expect(browsing.gazeDot).toBe(false)
  await expect(page.getByLabel('Show the live gaze dot (test mode)')).toHaveCount(0)
  expect(await video.evaluate((element) =>
    ((element as HTMLVideoElement).srcObject as MediaStream).getTracks().map((t) => t.readyState),
  )).toEqual(['live'])

  await leave.click()
  await expect(page.getByText('Thank you for participating')).toBeVisible()
  expect(await video.evaluate((element) => ({
    tracks: ((element as HTMLVideoElement).srcObject as MediaStream | null)
      ?.getTracks()
      .map((t) => t.readyState) ?? [],
    attached: element?.isConnected ?? false,
  }))).toEqual({ tracks: ['ended'], attached: false })

  // One attempt, recorded as it finished and then used, unchanged, to complete calibration.
  expect(backend.attempts).toHaveLength(1)
  expect(backend.completions).toEqual(backend.attempts)
  expect(backend.completions[0]).toMatchObject({
    schemaVersion: 1,
    sessionId: SESSION_ID,
    attemptNumber: 1,
    outcome: 'COMPLETED',
    trackingAvailable: true,
    quality: 'NOT_MEASURED',
    calibrationTargetsRequired: 9,
    viewportWidth: 1280,
    viewportHeight: 720,
  })
  expect(backend.batches).toEqual([])
  expect([...backend.tokens]).toEqual([SESSION_TOKEN])
  const order = backend.calls.filter((call) => !call.startsWith('GET'))
  expect(order).toEqual([
    `POST /api/participation/${STUDY_TOKEN}/sessions`,
    'PUT /api/participant-session/consent',
    `PUT /api/participant-session/calibration-attempts/${String(backend.attempts[0].attemptId)}`,
    'POST /api/participant-session/calibration-completion',
    'POST /api/participant-session/browsing-completion',
  ])
})
