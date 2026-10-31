// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createSessionClock } from '../../src/eyetracking/gaze/sessionClock'
import {
  clearBehaviorEvents, getBehaviorEvents, isBehaviorCollectionActive,
  startBehaviorCollection, stopBehaviorCollection, takeBehaviorEvents,
} from '../../src/interaction/behaviorCollector'

let now = 1000
const origin = 1_700_000_000_000
function start(pageId = 'feed') {
  const clock = createSessionClock({ now: () => now, wallClock: () => origin })
  startBehaviorCollection(clock, pageId)
  return clock
}
function target() {
  document.body.innerHTML = '<article data-component-id="post" data-component-type="PostWidget"><button data-component-id="post:like" data-component-type="PostLike"><span>Like</span></button></article>'
  return document.querySelector('span')!
}
function click(element: EventTarget = window) {
  element.dispatchEvent(new MouseEvent('click', { bubbles: true, clientX: 120, clientY: 240 }))
}

beforeEach(() => {
  now = 1000
  stopBehaviorCollection()
  clearBehaviorEvents()
  vi.spyOn(performance, 'now').mockImplementation(() => now)
  vi.spyOn(console, 'log').mockImplementation(() => {})
})
afterEach(() => {
  stopBehaviorCollection()
  clearBehaviorEvents()
  document.body.innerHTML = ''
  vi.restoreAllMocks()
})

describe('Behavior collection', () => {
  it('uses the shared session clock and the nearest nested component for a click', () => {
    const element = target()
    start(' feed ')
    now += 125
    click(element)
    expect(getBehaviorEvents()).toEqual([{
      type: 'click', timestamp: origin + 125, sessionElapsedMs: 125,
      pageId: 'feed', x: 120, y: 240, componentId: 'post:like', componentType: 'PostLike',
    }])
  })

  it('records an untracked or non-element click without inventing a component', () => {
    start()
    click(document.body)
    click(window)
    expect(getBehaviorEvents()).toHaveLength(2)
    for (const event of getBehaviorEvents()) {
      expect(event).toMatchObject({ componentId: null, componentType: null })
    }
  })

  it('records keyboard codes without typed text and ignores repeats and empty codes', () => {
    const element = target()
    start()
    element.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, code: 'KeyA', key: 'a' }))
    element.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, code: 'KeyA', repeat: true }))
    element.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'a' }))
    expect(getBehaviorEvents()).toHaveLength(1)
    expect(getBehaviorEvents()[0]).toMatchObject({ type: 'keydown', code: 'KeyA', componentId: 'post:like' })
    expect(getBehaviorEvents()[0]).not.toHaveProperty('key')
  })

  it('limits mouse movement to one event per 50 ms including the boundary', () => {
    const element = target()
    start()
    const move = () => element.dispatchEvent(new MouseEvent('mousemove', { bubbles: true, clientX: 30, clientY: 40 }))
    move()
    now += 49
    move()
    now += 1
    move()
    expect(getBehaviorEvents()).toHaveLength(2)
    expect(getBehaviorEvents()[1]).toMatchObject({ type: 'mousemove', x: 30, y: 40, componentId: 'post:like', sessionElapsedMs: 50 })
  })

  it('limits scrolling to one event per 100 ms and reads current offsets', () => {
    start()
    vi.spyOn(window, 'scrollX', 'get').mockReturnValue(15)
    vi.spyOn(window, 'scrollY', 'get').mockReturnValue(320)
    window.dispatchEvent(new Event('scroll'))
    now += 99
    window.dispatchEvent(new Event('scroll'))
    now += 1
    window.dispatchEvent(new Event('scroll'))
    expect(getBehaviorEvents()).toHaveLength(2)
    expect(getBehaviorEvents()[1]).toMatchObject({ type: 'scroll', scrollX: 15, scrollY: 320, componentId: null, componentType: null })
  })

  it('records leaving and returning to the document', () => {
    start()
    const hidden = vi.spyOn(document, 'hidden', 'get')
    hidden.mockReturnValue(true)
    document.dispatchEvent(new Event('visibilitychange'))
    hidden.mockReturnValue(false)
    document.dispatchEvent(new Event('visibilitychange'))
    expect(getBehaviorEvents().map(event => event.type === 'visibilitychange' ? event.hidden : null)).toEqual([true, false])
  })

  it('restarting does not duplicate listeners and uses the new page id', () => {
    start('first')
    start('second')
    click()
    expect(getBehaviorEvents()).toHaveLength(1)
    expect(getBehaviorEvents()[0].pageId).toBe('second')
    expect(isBehaviorCollectionActive()).toBe(true)
  })

  it('stops all five listeners while preserving events awaiting upload', () => {
    start()
    click()
    stopBehaviorCollection()
    click()
    window.dispatchEvent(new KeyboardEvent('keydown', { code: 'KeyA' }))
    window.dispatchEvent(new MouseEvent('mousemove'))
    window.dispatchEvent(new Event('scroll'))
    document.dispatchEvent(new Event('visibilitychange'))
    expect(isBehaviorCollectionActive()).toBe(false)
    expect(getBehaviorEvents()).toHaveLength(1)
  })

  it('returns copies for inspection and drains events in order', () => {
    start()
    click()
    now += 10
    click()
    const copy = getBehaviorEvents()
    copy[0].pageId = 'changed'
    expect(takeBehaviorEvents(1)[0].pageId).toBe('feed')
    expect(takeBehaviorEvents(0)).toEqual([])
    expect(takeBehaviorEvents()[0].sessionElapsedMs).toBe(10)
    expect(getBehaviorEvents()).toEqual([])
  })

  it('rejects an empty page id before starting collection', () => {
    expect(() => start('   ')).toThrow('non-empty pageId')
    expect(isBehaviorCollectionActive()).toBe(false)
  })
})
