import axios from 'axios'
import { describe, expect, it, vi } from 'vitest'
import client from '../../src/participant/api/participantHttpClient'
import { isStudyClosedError, notifyStudyClosed, subscribeToStudyClosure } from '../../src/participant/model/studyClosure'

describe('session-scoped study closure', () => {
  it('notifies only matching sessions and removes subscriptions on cleanup', () => {
    const first = vi.fn()
    const second = vi.fn()
    const stopFirst = subscribeToStudyClosure('a', first)
    const stopSecond = subscribeToStudyClosure('b', second)
    notifyStudyClosed('a')
    expect(first).toHaveBeenCalledOnce()
    expect(second).not.toHaveBeenCalled()
    stopFirst()
    notifyStudyClosed('a')
    expect(first).toHaveBeenCalledOnce()
    stopSecond()
  })

  it('recognizes HTTP 410 and the stable code without treating network failures as closure', () => {
    expect(isStudyClosedError({ isAxiosError: true, response: { status: 410, data: {} } })).toBe(true)
    expect(isStudyClosedError({ isAxiosError: true, response: { status: 409, data: { code: 'STUDY_CLOSED' } } })).toBe(true)
    expect(isStudyClosedError(new Error('network'))).toBe(false)
    expect(isStudyClosedError({ isAxiosError: true, response: { status: 503, data: {} } })).toBe(false)
  })

  it('broadcasts closure from the anonymous HTTP client using the request session token', async () => {
    const closed = vi.fn()
    const unsubscribe = subscribeToStudyClosure('client-session', closed)
    try {
      await expect(client.get('/api/participant-session/questionnaire/current', {
        headers: { 'X-Participant-Session-Token': 'client-session' },
        adapter: async config => {
          throw new axios.AxiosError('Closed', 'ERR_BAD_RESPONSE', config, undefined, {
            data: { code: 'STUDY_CLOSED' }, status: 410, statusText: 'Gone', headers: {}, config,
          })
        },
      })).rejects.toThrow('Closed')
      expect(closed).toHaveBeenCalledOnce()
    } finally { unsubscribe() }
  })
})
