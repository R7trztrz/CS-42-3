// @vitest-environment happy-dom

import { afterEach, describe, expect, it } from 'vitest'
import {
  clearStoredSessionToken,
  readStoredSessionToken,
  storeSessionToken,
} from '../../src/participant/model/participantSession'

describe('M5 participant session storage', () => {
  afterEach(() => {
    window.sessionStorage.clear()
  })

  it('returns null when nothing is stored for a study token', () => {
    expect(readStoredSessionToken('study-a')).toBeNull()
  })

  it('stores and reads a session token namespaced by study token', () => {
    storeSessionToken('study-a', 'token-a')
    storeSessionToken('study-b', 'token-b')

    expect(readStoredSessionToken('study-a')).toBe('token-a')
    expect(readStoredSessionToken('study-b')).toBe('token-b')
  })

  it('clears only the token for the given study token', () => {
    storeSessionToken('study-a', 'token-a')
    storeSessionToken('study-b', 'token-b')

    clearStoredSessionToken('study-a')

    expect(readStoredSessionToken('study-a')).toBeNull()
    expect(readStoredSessionToken('study-b')).toBe('token-b')
  })

  it('never throws when sessionStorage access fails', () => {
    const original = window.sessionStorage.getItem
    window.sessionStorage.getItem = () => {
      throw new Error('blocked')
    }

    expect(() => readStoredSessionToken('study-a')).not.toThrow()
    expect(readStoredSessionToken('study-a')).toBeNull()

    window.sessionStorage.getItem = original
  })
})
