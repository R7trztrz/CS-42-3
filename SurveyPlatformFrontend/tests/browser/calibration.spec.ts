import { test, expect } from '@playwright/test'
import type {} from './harness'

test.beforeEach(async ({ page }) => {
  await page.goto('/tests/browser/harness.html')
  await page.waitForFunction(() => !!window.fr49Harness)
})

test('calibration emits no gaze events; low-quality handoff continues tracking and session end stops every track', async ({
  page,
}) => {
  const uploads: string[] = []
  page.on('request', (request) => {
    if (request.method() === 'POST') uploads.push(request.url())
  })
  await page.evaluate(() => window.fr49Harness.start())
  await page.evaluate(() => window.fr49Harness.click())
  await page.evaluate(() => window.fr49Harness.skip())
  let state = await page.evaluate(() => window.fr49Harness.state())
  expect(state.gaze).toHaveLength(0)
  expect(state.attempt?.quality).toBe('POOR')
  expect(state.records).toHaveLength(1)
  expect(state.tracks).toEqual(['live', 'live'])
  expect(state.mouseEnabled).toBe(false)
  expect(uploads).toHaveLength(0)
  await page.evaluate(() => {
    window.fr49Harness.handoff()
    window.fr49Harness.emit()
  })
  state = await page.evaluate(() => window.fr49Harness.state())
  expect(state.gaze).toHaveLength(1)
  expect(state.gaze[0]).toMatchObject({ sessionId: 'browser-session', quality: 'POOR' })
  expect(state.begins).toBe(1)
  expect(state.resets).toBe(1)
  await page.evaluate(() => window.fr49Harness.endSession())
  state = await page.evaluate(() => window.fr49Harness.state())
  expect(state.tracks).toEqual(['ended', 'ended'])
  expect(state.videoPresent).toBe(false)
})

test('late permission completion after leaving the page stops actual tracks and removes the video', async ({
  page,
}) => {
  await page.evaluate(() => window.fr49Harness.start({ delayed: true }))
  await page.evaluate(() => window.fr49Harness.dispose())
  expect((await page.evaluate(() => window.fr49Harness.state())).phase).toBe('DISPOSED')
  await page.evaluate(() => window.fr49Harness.releaseStart())
  await expect
    .poll(async () => (await page.evaluate(() => window.fr49Harness.state())).tracks)
    .toEqual(['ended', 'ended'])
  expect((await page.evaluate(() => window.fr49Harness.state())).videoPresent).toBe(false)
})

test('late stream is cleaned even while model initialization remains unresolved', async ({
  page,
}) => {
  await page.evaluate(() => window.fr49Harness.start({ delayed: true, hangingModel: true }))
  await page.evaluate(() => window.fr49Harness.skip())
  await page.evaluate(() => window.fr49Harness.releaseStart())
  await expect
    .poll(async () => (await page.evaluate(() => window.fr49Harness.state())).tracks)
    .toEqual(['ended', 'ended'])
  await page.evaluate(() => window.fr49Harness.releaseModel())
  expect((await page.evaluate(() => window.fr49Harness.state())).records).toHaveLength(1)
})

test('camera rejection produces a reason and a completed promise without any gaze or media stream', async ({
  page,
}) => {
  await page.evaluate(() => window.fr49Harness.start({ denied: true }))
  const state = await page.evaluate(() => window.fr49Harness.state())
  expect(state.phase).toBe('FINISHED')
  expect(state.attempt).toMatchObject({
    outcome: 'UNAVAILABLE',
    unavailableReason: 'PERMISSION_DENIED',
  })
  expect(state.records).toHaveLength(1)
  expect(state.gaze).toHaveLength(0)
  expect(state.tracks).toHaveLength(0)
})

test('complete nine-point run records all points and disposal cleans resources', async ({
  page,
}) => {
  await page.evaluate(() => window.fr49Harness.start())
  await page.evaluate(() => window.fr49Harness.complete())
  const state = await page.evaluate(() => window.fr49Harness.state())
  expect(state.attempt?.calibrationTargetsMeasured).toBe(9)
  expect(state.training).toBe(45)
  expect(state.records).toHaveLength(1)
  expect(state.gaze).toHaveLength(0)
  await page.evaluate(() => window.fr49Harness.dispose())
  expect((await page.evaluate(() => window.fr49Harness.state())).tracks).toEqual(['ended', 'ended'])
})
