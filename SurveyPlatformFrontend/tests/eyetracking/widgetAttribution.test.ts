import { describe, expect, it } from 'vitest'
import {
  MAX_ATTRIBUTION_DEPTH,
  WIDGET_ID_ATTRIBUTE,
  WIDGET_SELECTOR,
  attributeGaze,
  findWidgetId,
} from '../../src/eyetracking'
import type { AttributableElement, ElementLocator } from '../../src/eyetracking'
import { fakeElement } from './gazeHelpers'

function locator(element: AttributableElement | null): ElementLocator {
  return { elementFromPoint: () => element }
}

describe('Widget attribution', () => {
  it('uses the attribute and selector the rendering contract names', () => {
    expect(WIDGET_ID_ATTRIBUTE).toBe('data-widget-id')
    expect(WIDGET_SELECTOR).toBe('[data-widget-id]')
  })

  it('attributes the hit element and trims the identifier', () => {
    expect(attributeGaze(locator(fakeElement('  question-7  ')), 10, 20)).toEqual({
      widgetId: 'question-7',
      attribution: 'WIDGET',
    })
  })

  it('walks ancestors, exactly as closest would', () => {
    const root = fakeElement('question-7')
    const middle = fakeElement(null, root)
    const leaf = fakeElement(null, middle)
    expect(findWidgetId(leaf)).toBe('question-7')
    expect(attributeGaze(locator(leaf), 0, 0).attribution).toBe('WIDGET')
  })

  it('skips an empty identifier and keeps walking, because an empty one is an injection defect', () => {
    const good = fakeElement('question-7')
    const blank = fakeElement('   ', good)
    expect(findWidgetId(blank)).toBe('question-7')
    expect(attributeGaze(locator(fakeElement('')), 0, 0)).toEqual({
      widgetId: null,
      attribution: 'UNRESOLVED',
    })
  })

  it('separates no element at the point from an element outside any widget', () => {
    expect(attributeGaze(locator(null), 0, 0)).toEqual({
      widgetId: null,
      attribution: 'NO_CONTENT',
    })
    expect(attributeGaze(locator(fakeElement(null)), 0, 0)).toEqual({
      widgetId: null,
      attribution: 'UNRESOLVED',
    })
  })

  it.each([NaN, Infinity, -Infinity])(
    'returns an annotation rather than throwing for coordinate %s',
    (coordinate) => {
      const thrower: ElementLocator = {
        elementFromPoint: () => {
          throw new Error('elementFromPoint must not be reached for a non-finite point.')
        },
      }
      expect(attributeGaze(thrower, coordinate, 0).attribution).toBe('UNRESOLVED')
      expect(attributeGaze(thrower, 0, coordinate).attribution).toBe('UNRESOLVED')
    },
  )

  it('terminates on a cyclic ancestor chain instead of stalling the sample handler', () => {
    const node: { getAttribute: () => string | null; parentElement: AttributableElement | null } = {
      getAttribute: () => null,
      parentElement: null,
    }
    node.parentElement = node
    expect(findWidgetId(node)).toBeNull()
    expect(MAX_ATTRIBUTION_DEPTH).toBe(200)
  })

  it('stops after the depth limit even when a widget sits beyond it', () => {
    let node = fakeElement('too-deep')
    for (let depth = 0; depth < MAX_ATTRIBUTION_DEPTH; depth += 1) node = fakeElement(null, node)
    expect(findWidgetId(node)).toBeNull()
  })

  it('returns frozen results so an annotation cannot be edited after the fact', () => {
    const result = attributeGaze(locator(fakeElement('question-7')), 0, 0)
    expect(Object.isFrozen(result)).toBe(true)
    expect(Object.isFrozen(attributeGaze(locator(null), 0, 0))).toBe(true)
  })

  it('reads only the widget attribute from the element', () => {
    const seen: string[] = []
    const element: AttributableElement = {
      getAttribute: (name) => {
        seen.push(name)
        return null
      },
      parentElement: null,
    }
    findWidgetId(element)
    expect(seen).toEqual(['data-widget-id'])
  })
})
