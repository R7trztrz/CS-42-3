export type BehaviorEvent =
  | {
      type: 'click'
      timestamp: number
      x: number
      y: number
      componentId: string | null
      componentType: string | null
    }
  | {
      type: 'keydown'
      timestamp: number
      code: string
      componentId: string | null
      componentType: string | null
    }

const events: BehaviorEvent[] = []
let collecting = false

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

function handleClick(event: MouseEvent) {
  const component = getTrackedComponent(event.target)

  events.push({
    type: 'click',
    timestamp: Date.now(),
    x: event.clientX,
    y: event.clientY,
    ...component,
  })
}

function handleKeyDown(event: KeyboardEvent) {
  if (event.repeat) {
    return
  }
  const component = getTrackedComponent(event.target)

  events.push({
    type: 'keydown',
    timestamp: Date.now(),
    code: event.code,
    ...component,
  })
}

export function startBehaviorCollection() {
  if (collecting) {
    return
  }

  collecting = true

  window.addEventListener(
    'click',
    handleClick,
    true,
  )

  window.addEventListener(
    'keydown',
    handleKeyDown,
    true,
  )
}

export function stopBehaviorCollection() {
  if (!collecting) {
    return
  }

  collecting = false

  window.removeEventListener(
    'click',
    handleClick,
    true,
  )

  window.removeEventListener(
    'keydown',
    handleKeyDown,
    true,
  )
}

export function getBehaviorEvents() {
  return [...events]
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
