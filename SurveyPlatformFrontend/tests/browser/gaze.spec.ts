import { test, expect } from '@playwright/test'
import type {} from './gazeHarness'

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 720 })
  await page.goto('/tests/browser/gazeHarness.html')
  await page.waitForFunction(() => !!window.fr50Harness)
})

test('attributes real layout three ways and keeps every point', async ({ page }) => {
  await page.evaluate(() => window.fr50Harness.start({ batchSize: 4 }))
  const points = await page.evaluate(() => window.fr50Harness.points())
  await page.evaluate((layout) => {
    window.fr50Harness.emit(layout.widget.x, layout.widget.y)
    window.fr50Harness.emit(layout.chrome.x, layout.chrome.y)
    window.fr50Harness.emit(layout.unmarked.x, layout.unmarked.y)
    window.fr50Harness.emit(layout.offscreen.x, layout.offscreen.y)
  }, points)
  await page.evaluate(() => window.fr50Harness.settle())
  const state = await page.evaluate(() => window.fr50Harness.state())
  expect(state.events).toHaveLength(4)
  expect(state.events.map((event) => event.attribution)).toEqual([
    'WIDGET',
    'UNRESOLVED',
    'UNRESOLVED',
    'NO_CONTENT',
  ])
  // The ancestor walk resolves a nested leaf to the widget root, as closest would.
  expect(state.events[0].widgetId).toBe('question-2')
  expect(state.events.slice(1).map((event) => event.widgetId)).toEqual([null, null, null])
  // Attribution is an annotation: the raw point survives all three outcomes.
  expect(state.events.map((event) => [event.x, event.y])).toEqual([
    [points.widget.x, points.widget.y],
    [points.chrome.x, points.chrome.y],
    [points.unmarked.x, points.unmarked.y],
    [points.offscreen.x, points.offscreen.y],
  ])
  expect(state.stats).toMatchObject({
    samplesReceived: 4,
    eventsProduced: 4,
    eventsDiscarded: 0,
    attribution: { WIDGET: 1, NO_CONTENT: 1, UNRESOLVED: 2 },
  })
})

test('records scroll so a point keeps its document coordinates', async ({ page }) => {
  await page.evaluate(() => window.fr50Harness.start({ batchSize: 1 }))
  const before = await page.evaluate(() => window.fr50Harness.points())
  await page.evaluate(() => {
    document.body.style.minHeight = '4000px'
    window.scrollTo(0, 200)
  })
  const after = await page.evaluate(() => window.fr50Harness.points())
  expect(after.unmarked.y).toBe(before.unmarked.y - 200)
  await page.evaluate((layout) => window.fr50Harness.emit(layout.unmarked.x, layout.unmarked.y), after)
  await page.evaluate(() => window.fr50Harness.settle())
  const [event] = (await page.evaluate(() => window.fr50Harness.state())).events
  expect(event.y).toBe(after.unmarked.y)
  expect(event.scrollY).toBe(200)
  // The same physical spot on the document, whatever the page was scrolled to.
  expect(event.pageY).toBe(before.unmarked.y)
  expect(event.viewportWidth).toBe(1280)
  expect(event.viewportHeight).toBe(720)
})

test('buffers in a worker and produces exactly what the main-thread fallback produces', async ({
  page,
}) => {
  const run = async (mainThread: boolean) => {
    await page.goto('/tests/browser/gazeHarness.html')
    await page.waitForFunction(() => !!window.fr50Harness)
    await page.evaluate((flag) => window.fr50Harness.start({ mainThread: flag, batchSize: 3 }), mainThread)
    const points = await page.evaluate(() => window.fr50Harness.points())
    await page.evaluate((layout) => {
      for (let index = 0; index < 7; index += 1) {
        window.fr50Harness.emit(layout.widget.x + index, layout.widget.y)
      }
    }, points)
    await page.evaluate(() => window.fr50Harness.stop())
    return page.evaluate(() => window.fr50Harness.state())
  }
  const worker = await run(false)
  const fallback = await run(true)
  expect(worker.mode).toBe('WORKER')
  expect(fallback.mode).toBe('MAIN_THREAD')
  expect(worker.events).toEqual(fallback.events)
  // Everything but the generated batch ID has to match, on both paths.
  const envelope = (batches: typeof worker.batches) =>
    batches.map((batch) => [
      batch.schemaVersion,
      batch.sessionId,
      batch.sequence,
      batch.eventCount,
      batch.droppedEvents,
      batch.droppedEventsTotal,
    ])
  expect(envelope(worker.batches)).toEqual(envelope(fallback.batches))
  expect(worker.batches.map((batch) => batch.sequence)).toEqual([1, 2, 3])
  expect(worker.events).toHaveLength(7)
  expect(worker.engineStopped).toBe(true)
  expect(worker.issues).toEqual([])
})

test('keeps the raw camera feed out of the payload and off the network', async ({ page }) => {
  const posts: string[] = []
  page.on('request', (request) => {
    if (request.method() !== 'GET') posts.push(`${request.method()} ${request.url()}`)
  })
  await page.evaluate(() => window.fr50Harness.start({ batchSize: 5 }))
  const points = await page.evaluate(() => window.fr50Harness.points())
  await page.evaluate((layout) => {
    for (let index = 0; index < 5; index += 1) {
      window.fr50Harness.emit(layout.widget.x, layout.widget.y + index)
    }
  }, points)
  await page.evaluate(() => window.fr50Harness.settle())
  const state = await page.evaluate(() => window.fr50Harness.state())
  expect(posts).toEqual([])
  expect(state.payloads).toHaveLength(1)
  for (const payload of state.payloads) {
    expect(payload).not.toMatch(/data:image|blob:|ImageData|videoinput|srcObject/)
    expect(payload).not.toMatch(/NaN|Infinity/)
    expect(JSON.parse(payload)).toHaveLength(5)
  }
  expect(await page.evaluate(() => !!document.getElementById('webgazerVideoFeed'))).toBe(false)
})

test('costs the main thread little per prediction while the worker does the buffering', async ({
  page,
}) => {
  await page.evaluate(() => window.fr50Harness.start({ batchSize: 100 }))
  const measurement = await page.evaluate(() => window.fr50Harness.emitMany(600))
  await page.evaluate(() => window.fr50Harness.settle())
  const state = await page.evaluate(() => window.fr50Harness.state())
  expect(state.mode).toBe('WORKER')
  expect(measurement.count).toBe(600)
  // One elementFromPoint, one layout read and one postMessage; two orders of magnitude of headroom.
  expect(measurement.perSampleMs).toBeLessThan(2)
  expect(state.stats?.eventsProduced).toBe(600)
  expect(state.stats?.eventsDiscarded).toBe(0)
})

test('drops the oldest events when the pipeline stalls, and says how many', async ({ page }) => {
  await page.evaluate(() => window.fr50Harness.start({ batchSize: 2, maxEvents: 2 }))
  const points = await page.evaluate(() => window.fr50Harness.points())
  await page.evaluate((layout) => {
    for (let index = 0; index < 9; index += 1) window.fr50Harness.emit(layout.widget.x, layout.widget.y)
  }, points)
  // While the queue is still backed up, the flush heartbeat already reports the loss and the depth.
  await page.evaluate(() => window.fr50Harness.flush())
  await page.evaluate(() => window.fr50Harness.settle())
  const midRun = await page.evaluate(() => window.fr50Harness.state())
  expect(midRun.stats?.eventsDiscarded).toBeGreaterThan(0)
  expect(midRun.stats?.eventsBuffered).toBeLessThanOrEqual(2)
  await page.evaluate(() => window.fr50Harness.stop())
  const state = await page.evaluate(() => window.fr50Harness.state())
  expect(state.stats?.eventsProduced).toBe(9)
  expect(state.stats?.eventsDiscarded).toBeGreaterThan(0)
  expect(state.events.length + (state.stats?.eventsDiscarded ?? 0)).toBe(9)
  expect(state.batches.at(-1)?.droppedEventsTotal).toBe(state.stats?.eventsDiscarded)
})
