// @vitest-environment happy-dom

import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const { route, getParticipation, createSession, completeBrowsing, sessionView } = vi.hoisted(() => ({
  route: { token: 'study-a' },
  getParticipation: vi.fn(),
  createSession: vi.fn(),
  completeBrowsing: vi.fn(),
  sessionView: { phase: '' },
}))
vi.mock('react-router-dom', () => ({ useParams: () => ({ token: route.token }) }))
vi.mock('../../src/services/studyApi', () => ({ getParticipation }))
vi.mock('@craftjs/core', async (importOriginal) => ({
  ...await importOriginal<typeof import('@craftjs/core')>(),
  Frame: () => <div data-testid="published-interface">Published study interface</div>,
}))
vi.mock('../../src/participant/hooks/useParticipantSession', () => ({
  useParticipantSession: () => ({
    stage: sessionView.phase ? 'active' : 'landing',
    session: sessionView.phase ? {
      status: 'IN_PROGRESS', phase: sessionView.phase,
      sessionId: 'session-1',
      consentDocument: { title: 'Consent document', content: 'Study information', approvedForProduction: true },
    } : null,
    sessionToken: null, createSession, completeBrowsing, refreshSession: vi.fn(),
  }),
}))

import ParticipationPage from '../../src/pages/participant/ParticipationPage'
import { deferred } from './fixtures'

const participation = (title: string) => ({
  title, description: null, eyeTrackingEnabled: false, questionnaireEnabled: false,
  theme: null, content: null,
})

describe('participation page route changes', () => {
  beforeEach(() => { route.token = 'study-a'; sessionView.phase = ''; sessionStorage.clear(); vi.resetAllMocks() })
  afterEach(cleanup)

  it.each([
    ['CONSENT', 'Informed consent'],
    ['CALIBRATION', 'Eye-tracking setup'],
    ['BROWSING', 'Eye-tracking setup'],
    ['QUESTIONNAIRE', 'Final questionnaire'],
  ])('keeps the steps visible and highlights the restored %s phase', async (phase, label) => {
    sessionView.phase = phase
    getParticipation.mockResolvedValueOnce(participation('Linked study'))
    const { container } = render(<ParticipationPage />)
    await screen.findByText('Linked study')
    expect(screen.getByRole('list', { name: 'Study steps' })).not.toBeNull()
    expect(container.querySelector('[aria-current="step"]')?.textContent).toContain(label)
    expect(screen.queryByRole('button', { name: 'Begin study' })).toBeNull()
  })

  it('shows preparation even when the server skips calibration for an eye-tracking-disabled study', async () => {
    sessionView.phase = 'BROWSING'
    getParticipation.mockResolvedValueOnce(participation('No eye tracking'))
    render(<ParticipationPage />)
    await screen.findByRole('heading', { name: "Let's prepare your camera" })
    expect(screen.getByText(/Eye tracking is not enabled for this study/)).not.toBeNull()
    expect(screen.getByRole('button', { name: 'Continue to activity' })).not.toBeNull()
    expect(screen.queryByRole('button', { name: 'Finish browsing' })).toBeNull()
  })

  it('waits for Start activity before mounting the published interface or allowing browsing completion', async () => {
    sessionView.phase = 'BROWSING'
    getParticipation.mockResolvedValue(participation('Activity study'))
    const { container, unmount } = render(<ParticipationPage />)
    await screen.findByRole('button', { name: 'Continue to activity' })
    fireEvent.click(screen.getByRole('button', { name: 'Continue to activity' }))
    expect(screen.getByRole('heading', { name: 'Ready to explore the feed?' })).not.toBeNull()
    expect(container.querySelector('[aria-current="step"]')?.textContent).toContain('Social media activity')
    expect(screen.queryByTestId('published-interface')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Finish browsing' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Start activity' }))
    expect(screen.getByTestId('published-interface')).not.toBeNull()
    expect(screen.queryByRole('button', { name: 'Start activity' })).toBeNull()
    expect(completeBrowsing).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Finish browsing' }))
    expect(completeBrowsing).toHaveBeenCalledTimes(1)
    unmount()
    render(<ParticipationPage />)
    await screen.findByRole('button', { name: 'Start activity' })
    expect(screen.queryByTestId('published-interface')).toBeNull()
  })

  it('uses the linked study title and description in the introduction and starts its session', async () => {
    getParticipation.mockResolvedValueOnce({ ...participation('Campus study'), description: 'First line\nSecond line' })
    render(<ParticipationPage />)
    await screen.findByRole('heading', { name: 'Campus study', level: 1 })
    expect(screen.getByText(/First line/).textContent).toBe('First line\nSecond line')
    expect(screen.queryByText('How people explore social media content')).toBeNull()
    expect(getParticipation).toHaveBeenCalledWith('study-a')
    fireEvent.click(screen.getByRole('button', { name: 'Begin study' }))
    expect(createSession).toHaveBeenCalledTimes(1)
  })

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
