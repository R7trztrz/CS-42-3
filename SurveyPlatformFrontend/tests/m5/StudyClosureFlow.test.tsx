// @vitest-environment happy-dom

import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const { http, getParticipation, owner } = vi.hoisted(() => ({
  http: { get: vi.fn(), post: vi.fn(), put: vi.fn() }, getParticipation: vi.fn(),
  owner: { subscribe: vi.fn(() => () => {}), getSnapshot: vi.fn(), endForStudyClosure: vi.fn(), beginTracking: vi.fn(), release: vi.fn() },
}))
vi.mock('react-router-dom', () => ({ useParams: () => ({ token: 'study-a' }) }))
vi.mock('../../src/services/studyApi', () => ({ getParticipation }))
vi.mock('../../src/participant/api/participantHttpClient', () => ({ default: http }))
vi.mock('@craftjs/core', async importOriginal => ({
  ...await importOriginal<typeof import('@craftjs/core')>(), Frame: () => <div>Published feed</div>,
}))
vi.mock('../../src/eyetracking/participant', () => ({
  EYE_TRACKING_TEST_MODE: false,
  createHttpEyeTrackingTransport: vi.fn(),
  ParticipantEyeTracking: vi.fn(function () { return owner }),
  leaseParticipantEyeTracking: vi.fn(() => ({ owner, release: owner.release })),
}))

import ParticipationPage from '../../src/pages/participant/ParticipationPage'
import { notifyStudyClosed } from '../../src/participant/model/studyClosure'
import { storeSessionToken } from '../../src/participant/model/participantSession'
import { axiosError, makeQuestion, makeSessionResponse } from './fixtures'

beforeEach(() => {
  vi.clearAllMocks()
  sessionStorage.clear()
  storeSessionToken('study-a', 'session-a')
  owner.getSnapshot.mockReturnValue({ stage: 'IDLE', attempt: null })
  getParticipation.mockResolvedValue({ title: 'Campus study', description: '', eyeTrackingEnabled: true, questionnaireEnabled: true, theme: null, content: null })
})
afterEach(cleanup)

describe('unified participant closure', () => {
  it('ends the whole page and releases the eye-tracking owner on a same-session upload notification', async () => {
    http.get.mockResolvedValue({ data: makeSessionResponse('BROWSING', { eyeTrackingEnabled: true }) })
    render(<ParticipationPage />)
    await screen.findByRole('button', { name: 'Continue to activity' })
    act(() => notifyStudyClosed('other-session'))
    expect(screen.queryByRole('heading', { name: 'This study is closed' })).toBeNull()
    act(() => notifyStudyClosed('session-a'))
    await screen.findByRole('heading', { name: 'This study is closed' })
    expect(owner.endForStudyClosure).toHaveBeenCalled()
    expect(screen.queryByRole('button')).toBeNull()
    expect(screen.queryByText('Thank you for participating')).toBeNull()
  })

  it('ends the whole page when a questionnaire answer receives STUDY_CLOSED', async () => {
    http.get.mockImplementation((url: string) => Promise.resolve({ data: url.endsWith('/questionnaire/current')
      ? { currentQuestion: makeQuestion(), readyToSubmit: false }
      : makeSessionResponse('QUESTIONNAIRE') }))
    http.put.mockRejectedValue(axiosError(410, 'STUDY_CLOSED'))
    render(<ParticipationPage />)
    fireEvent.click(await screen.findByLabelText('Yes'))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    await screen.findByRole('heading', { name: 'This study is closed' })
    expect(screen.queryByRole('radio')).toBeNull()
    expect(screen.queryByRole('button')).toBeNull()
    expect(screen.queryByText('You have answered every question.')).toBeNull()
  })
})
