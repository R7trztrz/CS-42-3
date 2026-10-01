import type { GazeAttribution, GazeViewport } from './types'

/** The attribute the rendering layer injects onto every craft.js widget root. */
export const WIDGET_ID_ATTRIBUTE = 'data-widget-id'
export const WIDGET_SELECTOR = `[${WIDGET_ID_ATTRIBUTE}]`
/** Bounds the ancestor walk so a malformed or cyclic tree cannot stall the sample handler. */
export const MAX_ATTRIBUTION_DEPTH = 200

/** The structural slice of Element this module needs; a real Element satisfies it. */
export interface AttributableElement {
  getAttribute(name: string): string | null
  readonly parentElement: AttributableElement | null
}

export interface ElementLocator {
  elementFromPoint(x: number, y: number): AttributableElement | null
}

export interface GazeEnvironment extends ElementLocator {
  readViewport(): GazeViewport
}

export interface WidgetAttributionResult {
  readonly widgetId: string | null
  readonly attribution: GazeAttribution
}

export const NO_CONTENT_ATTRIBUTION: WidgetAttributionResult = Object.freeze({
  widgetId: null,
  attribution: 'NO_CONTENT',
})
export const UNRESOLVED_ATTRIBUTION: WidgetAttributionResult = Object.freeze({
  widgetId: null,
  attribution: 'UNRESOLVED',
})

/**
 * Equivalent to closest('[data-widget-id]'), with one deliberate difference: an ancestor whose
 * attribute is empty or whitespace is skipped rather than accepted, because an empty identifier
 * is an injection defect and must not be written into the data as if it identified a widget.
 */
export function findWidgetId(element: AttributableElement | null): string | null {
  let node = element
  for (let depth = 0; node && depth < MAX_ATTRIBUTION_DEPTH; depth += 1) {
    const value = node.getAttribute(WIDGET_ID_ATTRIBUTE)
    if (value !== null) {
      const widgetId = value.trim()
      if (widgetId) return widgetId
    }
    node = node.parentElement
  }
  return null
}

/**
 * Attribution is an added annotation only. It never decides whether an event is recorded, and the
 * caller keeps the raw coordinates whatever this returns.
 */
export function attributeGaze(
  locator: ElementLocator,
  x: number,
  y: number,
): WidgetAttributionResult {
  if (!Number.isFinite(x) || !Number.isFinite(y)) return UNRESOLVED_ATTRIBUTION
  const element = locator.elementFromPoint(x, y)
  if (!element) return NO_CONTENT_ATTRIBUTION
  const widgetId = findWidgetId(element)
  return widgetId === null
    ? UNRESOLVED_ATTRIBUTION
    : Object.freeze({ widgetId, attribution: 'WIDGET' as const })
}

/** Reads live layout from one window. Nothing else in this module touches a global. */
export function browserGazeEnvironment(view: Window): GazeEnvironment {
  return {
    readViewport: () =>
      Object.freeze({
        width: view.innerWidth,
        height: view.innerHeight,
        devicePixelRatio: view.devicePixelRatio,
        scrollX: view.scrollX,
        scrollY: view.scrollY,
      }),
    elementFromPoint: (x, y) => view.document.elementFromPoint(x, y),
  }
}
