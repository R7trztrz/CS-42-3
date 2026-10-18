import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'
import { collectGazeBatches } from '../../src/eyetracking'
import type { CalibrationAttempt, GazeEventBatch } from '../../src/eyetracking'
import { fakeElement, settle, setupCollector } from './gazeHelpers'
import { flush, setup } from './helpers'

/**
 * The backend integration tests post these exact files, so both sides are checked against output
 * the real FR-49 controller and FR-50 collector produce rather than a hand-written copy.
 * Regenerate with UPDATE_EYE_TRACKING_FIXTURES=1 after a deliberate wire-format change.
 */
const FIXTURE_DIR = fileURLToPath(
  new URL('../../../SurveyPlatformBackend/src/test/resources/eyetracking/', import.meta.url),
)
const SESSION_ID = '11111111-1111-4111-8111-111111111111'
const ATTEMPT_ID = '22222222-2222-4222-8222-222222222222'
const BATCH_ID = '33333333-3333-4333-8333-333333333333'
const session = { sessionId: SESSION_ID, consentGiven: true, eyeTrackingEnabled: true }

async function realAttempt(): Promise<CalibrationAttempt> {
  const { controller, complete } = setup({ session, createAttemptId: () => ATTEMPT_ID })
  const running = controller.start()
  await flush()
  complete(6)
  return running
}

async function realBatch(attempt: CalibrationAttempt): Promise<GazeEventBatch> {
  const sink = collectGazeBatches(SESSION_ID)
  const { collector, world, emit } = setupCollector({
    session,
    sink,
    calibration: attempt,
    pageId: 'feed',
    batchSize: 3,
  })
  const post = fakeElement('node-post-1')
  world.setViewport({ scrollY: 120 })
  world.setHit((x) => {
    if (x < 100) return fakeElement('node-post-1:like', post)
    if (x < 200) return null
    return fakeElement(null)
  })
  collector.start()
  emit(50, 60)
  emit(150, 60)
  emit(250, 60)
  await settle()
  await collector.stop()
  expect(sink.batches).toHaveLength(1)
  // The buffer assigns a random UUID; pin it so the file is stable.
  return { ...sink.batches[0], batchId: BATCH_ID }
}

function checkFixture(name: string, actual: unknown) {
  const file = `${FIXTURE_DIR}${name}`
  if (process.env.UPDATE_EYE_TRACKING_FIXTURES === '1') {
    mkdirSync(FIXTURE_DIR, { recursive: true })
    writeFileSync(file, `${JSON.stringify(actual, null, 2)}\n`)
  }
  expect(JSON.parse(readFileSync(file, 'utf8'))).toEqual(JSON.parse(JSON.stringify(actual)))
}

describe('eye-tracking wire contract fixtures', () => {
  it('match what the calibration controller and gaze collector serialize', async () => {
    const attempt = await realAttempt()
    expect(attempt).toMatchObject({ outcome: 'COMPLETED', quality: 'GOOD', trackingAvailable: true })
    const batch = await realBatch(attempt)
    const events = JSON.parse(batch.payload) as { attribution: string }[]
    expect(events.map((event) => event.attribution)).toEqual(['WIDGET', 'NO_CONTENT', 'UNRESOLVED'])

    checkFixture('calibration-attempt.json', attempt)
    checkFixture('gaze-batch.json', batch)
  })
})
