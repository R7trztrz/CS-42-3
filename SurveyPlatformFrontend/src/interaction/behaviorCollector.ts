import type { SessionClock } from '../eyetracking/gaze/sessionClock'

export type BehaviorEvent =
  | {
      type: 'click'
      timestamp: number
      sessionElapsedMs: number
      pageId: string
      x: number
      y: number
      componentId: string | null
      componentType: string | null
    }
  | {
      type: 'keydown'
      timestamp: number
      sessionElapsedMs: number
      pageId: string
      code: string
      componentId: string | null
      componentType: string | null
    }
  | {
      type: 'mousemove'
      timestamp: number
      sessionElapsedMs: number
      pageId: string
      x: number
      y: number
      componentId: string | null
      componentType: string | null
    }
  | {
      type: 'scroll'
      timestamp: number
      sessionElapsedMs: number
      pageId: string
      scrollX: number
      scrollY: number
      componentId: null
      componentType: null
    }
  | {
      type: 'visibilitychange'
      timestamp: number
      sessionElapsedMs: number
      pageId: string
      hidden: boolean
      componentId: null
      componentType: null
    }

const events: BehaviorEvent[] = []

let collecting = false
let sessionClock: SessionClock | null = null
let currentPageId = ''
let lastMouseMoveAt = 0
let lastScrollAt = 0

const MOUSEMOVE_INTERVAL_MS = 50
const SCROLL_INTERVAL_MS = 100

function getTrackedComponent(target: EventTarget | null) {
  if (!(target instanceof Element)) {
    return {
      componentId: null,
      componentType: null,
    }
  }

  const element = target.closest<HTMLElement>('[data-component-id]')

  return {
    componentId: element?.dataset.componentId ?? null,
    componentType: element?.dataset.componentType ?? null,
  }
}

function getCommonFields() {
  if (!sessionClock) {
    throw new Error('Behavior collection requires a SessionClock.')
  }

  if (!currentPageId.trim()) {
    throw new Error('Behavior collection requires a pageId.')
  }

  const reading = sessionClock.now()

  return {
    timestamp: Math.round(reading.wallMs),
    sessionElapsedMs: reading.sessionElapsedMs,
    pageId: currentPageId,
  }
}

function handleClick(event: MouseEvent) {
  const component = getTrackedComponent(event.target)

  const behaviorEvent: BehaviorEvent = {
    type: 'click',
    ...getCommonFields(),
    x: event.clientX,
    y: event.clientY,
    ...component,
  }

  events.push(behaviorEvent)

  if (import.meta.env.DEV) {
    console.log('Behavior click captured.', behaviorEvent)
  }
}

function handleKeyDown(event: KeyboardEvent) {
  if (event.repeat || !event.code) {
    return
  }

  const component = getTrackedComponent(event.target)

  const behaviorEvent: BehaviorEvent = {
    type: 'keydown',
    ...getCommonFields(),
    code: event.code,
    ...component,
  }

  events.push(behaviorEvent)

  if (import.meta.env.DEV) {
    console.log('Behavior keydown captured.', behaviorEvent)
  }
}

function handleMouseMove(event: MouseEvent) {
  const now = performance.now()

  if (now - lastMouseMoveAt < MOUSEMOVE_INTERVAL_MS) {
    return
  }

  lastMouseMoveAt = now

  const component = getTrackedComponent(event.target)

  events.push({
    type: 'mousemove',
    ...getCommonFields(),
    x: event.clientX,
    y: event.clientY,
    ...component,
  })
}

function handleScroll() {
  const now = performance.now()

  if (now - lastScrollAt < SCROLL_INTERVAL_MS) {
    return
  }

  lastScrollAt = now

  events.push({
    type: 'scroll',
    ...getCommonFields(),
    scrollX: window.scrollX,
    scrollY: window.scrollY,
    componentId: null,
    componentType: null,
  })
}

function handleVisibilityChange() {
  events.push({
    type: 'visibilitychange',
    ...getCommonFields(),
    hidden: document.hidden,
    componentId: null,
    componentType: null,
  })
}

function addBehaviorListeners() {
  window.addEventListener('click', handleClick, true)
  window.addEventListener('keydown', handleKeyDown, true)
  window.addEventListener('mousemove', handleMouseMove, true)
  window.addEventListener('scroll', handleScroll, true)
  document.addEventListener('visibilitychange', handleVisibilityChange, true)
}

function removeBehaviorListeners() {
  window.removeEventListener('click', handleClick, true)
  window.removeEventListener('keydown', handleKeyDown, true)
  window.removeEventListener('mousemove', handleMouseMove, true)
  window.removeEventListener('scroll', handleScroll, true)
  document.removeEventListener('visibilitychange', handleVisibilityChange, true)
}

export function startBehaviorCollection(
  clock: SessionClock,
  pageId: string,
) {
  const normalizedPageId = pageId.trim()

  if (!normalizedPageId) {
    throw new Error('Behavior collection requires a non-empty pageId.')
  }

  removeBehaviorListeners()

  sessionClock = clock
  currentPageId = normalizedPageId
  collecting = true
  lastMouseMoveAt = 0
  lastScrollAt = 0

  addBehaviorListeners()

  if (import.meta.env.DEV) {
    console.log('Behavior collection started.', {
      pageId: currentPageId,
    })
  }
}

export function stopBehaviorCollection() {
  removeBehaviorListeners()

  collecting = false
  sessionClock = null
  currentPageId = ''
  lastMouseMoveAt = 0
  lastScrollAt = 0

  if (import.meta.env.DEV) {
    console.log('Behavior collection stopped.')
  }
}

export function getBehaviorEvents() {
  return events.map((event) => ({
    ...event,
  }))
}

export function takeBehaviorEvents(limit = events.length) {
  if (!Number.isInteger(limit) || limit < 1) {
    return []
  }

  return events.splice(0, limit)
}

export function clearBehaviorEvents() {
  events.length = 0
}

export function isBehaviorCollectionActive() {
  return collecting
}

if (import.meta.env.DEV) {
  Object.assign(window, {
    getBehaviorEvents,
    clearBehaviorEvents,
    isBehaviorCollectionActive,
  })
}
