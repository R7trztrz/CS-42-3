// @vitest-environment happy-dom

import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const { route, getParticipation } = vi.hoisted(() => ({
  route: { token: 'study-a' },
  getParticipation: vi.fn(),
}))
vi.mock('react-router-dom', () => ({ useParams: () => ({ token: route.token }) }))
vi.mock('../../src/services/studyApi', () => ({ getParticipation }))
vi.mock('../../src/participant/hooks/useParticipantSession', () => ({
  useParticipantSession: () => ({ stage: 'landing', session: null, sessionToken: null }),
}))

import ParticipationPage from '../../src/pages/participant/ParticipationPage'
import { deferred } from './fixtures'

const participation = (title: string) => ({
  title, description: null, eyeTrackingEnabled: false, questionnaireEnabled: false,
  theme: null, content: null,
})

describe('participation page route changes', () => {
  beforeEach(() => { route.token = 'study-a'; vi.resetAllMocks() })
  afterEach(cleanup)

  it('hides old study metadata while the new study is loading', async () => {
    const next = deferred<{ title: string; description: null; eyeTrackingEnabled: boolean; questionnaireEnabled: boolean; theme: null; content: null }>()
    getParticipation.mockResolvedValueOnce(participation('Study A')).mockReturnValueOnce(next.promise)
    const { rerender } = render(<ParticipationPage />)
    await screen.findByText('Study A')
    route.token = 'study-b'
    rerender(<ParticipationPage />)
    expect(screen.queryByText('Study A')).toBeNull()
    expect(screen.getByRole('status').textContent).toContain('Loading study')
    await act(async () => { next.resolve(participation('Study B')) })
    await screen.findByText('Study B')
  })

  it('clears an old unavailable error when a valid study is opened', async () => {
    getParticipation.mockRejectedValueOnce(new Error('Unavailable')).mockResolvedValueOnce(participation('Study B'))
    const { rerender } = render(<ParticipationPage />)
    await screen.findByText('Study unavailable')
    route.token = 'study-b'
    rerender(<ParticipationPage />)
    await screen.findByText('Study B')
    expect(screen.queryByText('Study unavailable')).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('ignores a late public-study response after the route changes', async () => {
    const old = deferred<ReturnType<typeof participation>>()
    getParticipation.mockReturnValueOnce(old.promise).mockResolvedValueOnce(participation('Study B'))
    const { rerender } = render(<ParticipationPage />)
    await waitFor(() => expect(getParticipation).toHaveBeenCalledTimes(1))
    route.token = 'study-b'
    rerender(<ParticipationPage />)
    await screen.findByText('Study B')
    await act(async () => { old.resolve(participation('Study A')) })
    expect(screen.queryByText('Study A')).toBeNull()
    expect(screen.getByText('Study B')).not.toBeNull()
  })
})
