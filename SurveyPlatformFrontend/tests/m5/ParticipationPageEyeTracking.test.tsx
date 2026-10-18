// @vitest-environment happy-dom

import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { CalibrationAttempt } from '../../src/eyetracking'
import type { ParticipantEyeTrackingSnapshot } from '../../src/eyetracking/participant'

// M6 wiring on the participant page: which phases hold the eye-tracking owner, what the set-up
// review says about the camera, when gaze collection starts, and that queued gaze is delivered
// before the session leaves BROWSING. The owner itself is covered in tests/eyetracking.

const { route, getParticipation, completeBrowsing, abandon, sessionView, owner, ownerRequests } = vi.hoisted(() => ({
  route: { token: 'study-a' },
  getParticipation: vi.fn(),
  completeBrowsing: vi.fn(),
  abandon: vi.fn(),
  sessionView: { phase: 'BROWSING', eyeTrackingEnabled: true },
  owner: {
    snapshot: null as unknown,
    beginTracking: vi.fn(),
    stopTracking: vi.fn(),
    setGazeDotVisible: vi.fn(),
    cameraStream: vi.fn(),
  },
  ownerRequests: [] as boolean[],
}))
vi.mock('react-router-dom', () => ({ useParams: () => ({ token: route.token }) }))
vi.mock('../../src/services/studyApi', () => ({ getParticipation }))
vi.mock('@craftjs/core', async (importOriginal) => ({
  ...await importOriginal<typeof import('@craftjs/core')>(),
  Frame: () => <div data-testid="published-interface">Published study interface</div>,
}))
vi.mock('../../src/participant/hooks/useParticipantSession', () => ({
  useParticipantSession: () => ({
    stage: 'active',
    session: {
      status: 'IN_PROGRESS',
      phase: sessionView.phase,
      sessionId: 'session-1',
      eyeTrackingEnabled: sessionView.eyeTrackingEnabled,
      calibrationCompletedAt: sessionView.phase === 'BROWSING' ? '2026-10-18T00:00:00Z' : null,
      consentDocument: { title: 'Consent document', content: 'Study information', approvedForProduction: true },
    },
    sessionToken: 'session-token',
    isActionPending: false,
    actionErrorMessage: null,
    createSession: vi.fn(),
    completeCalibration: vi.fn(),
    refreshSession: vi.fn(),
    completeBrowsing,
    abandon,
  }),
}))
vi.mock('../../src/participant/hooks/useParticipantEyeTracking', () => ({
  useParticipantEyeTracking: (_sessionId: string | null, _sessionToken: string | null, active: boolean) => {
    ownerRequests.push(active)
    return active ? owner : null
  },
  useEyeTrackingSnapshot: (current: unknown) => (current ? owner.snapshot : null),
}))

import ParticipationPage from '../../src/pages/participant/ParticipationPage'
import { deferred } from './fixtures'

function snapshot(overrides: Partial<ParticipantEyeTrackingSnapshot> = {}): ParticipantEyeTrackingSnapshot {
  return {
    stage: 'FINISHED',
    target: null,
    progress: null,
    attempt: { trackingAvailable: true, outcome: 'COMPLETED', quality: 'GOOD' } as CalibrationAttempt,
    attemptsRemaining: 1,
    canRecalibrate: false,
    collectorStats: null,
    gazeDotVisible: false,
    canToggleGazeDot: false,
    ...overrides,
  }
}

async function openFeed() {
  fireEvent.click(await screen.findByRole('button', { name: 'Continue to activity' }))
  fireEvent.click(screen.getByRole('button', { name: 'Start activity' }))
  expect(screen.getByTestId('published-interface')).not.toBeNull()
}

describe('participation page eye tracking', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    route.token = 'study-a'
    sessionView.phase = 'BROWSING'
    sessionView.eyeTrackingEnabled = true
    owner.snapshot = snapshot()
    ownerRequests.length = 0
    sessionStorage.clear()
    getParticipation.mockResolvedValue({
      title: 'Eye study', description: null, eyeTrackingEnabled: true, questionnaireEnabled: false,
      theme: null, content: null,
    })
  })
  afterEach(cleanup)

  it.each([
    ['CONSENT', true, false],
    ['CALIBRATION', true, true],
    ['BROWSING', true, true],
    ['BROWSING', false, false],
  ])('in the %s phase with eye tracking %s, holds the owner: %s', async (phase, enabled, active) => {
    sessionView.phase = phase
    sessionView.eyeTrackingEnabled = enabled
    render(<ParticipationPage />)
    await screen.findByText('Eye study')
    expect(ownerRequests.length).toBeGreaterThan(0)
    expect(ownerRequests.every((request) => request === active)).toBe(true)
  })

  it('says on the set-up review that the camera stays on for the feed, and shows it', async () => {
    const stream = new MediaStream()
    owner.cameraStream.mockReturnValue(stream)
    render(<ParticipationPage />)

    expect(await screen.findByText('Calibration confirmed')).not.toBeNull()
    expect(screen.getByText(/Eye tracking runs while you browse the feed/)).not.toBeNull()
    expect(screen.getByText('Your camera stays on until you finish browsing')).not.toBeNull()
    expect(screen.getByText('On', { selector: 'small' })).not.toBeNull()
    expect((screen.getByLabelText('Live camera preview') as HTMLVideoElement).srcObject).toBe(stream)
  })

  it.each([
    ['the camera was unavailable', { attempt: { trackingAvailable: false } as CalibrationAttempt }],
    ['the page was reloaded after calibration', { stage: 'IDLE' as const, attempt: null }],
  ])('says eye tracking is off when %s', async (_case, overrides) => {
    owner.snapshot = snapshot(overrides)
    owner.cameraStream.mockReturnValue(null)
    render(<ParticipationPage />)

    expect(await screen.findByText('Eye tracking is off for this session')).not.toBeNull()
    expect(screen.queryByLabelText('Live camera preview')).toBeNull()
    expect(screen.getByText('Off', { selector: 'small' })).not.toBeNull()
    expect(screen.getByText('Camera is not active on this screen')).not.toBeNull()
    expect(screen.queryByText('Your camera stays on until you finish browsing')).toBeNull()
  })

  it('starts collecting gaze only once the feed is shown', async () => {
    render(<ParticipationPage />)

    const toActivity = await screen.findByRole('button', { name: 'Continue to activity' })
    expect(owner.beginTracking).not.toHaveBeenCalled()

    fireEvent.click(toActivity)
    expect(screen.getByRole('heading', { name: 'Ready to explore the feed?' })).not.toBeNull()
    expect(owner.beginTracking).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Start activity' }))
    expect(screen.getByTestId('published-interface')).not.toBeNull()
    expect(owner.beginTracking).toHaveBeenCalledTimes(1)
  })

  it.each([
    ['Finish browsing', completeBrowsing],
    ['Leave study', abandon],
  ])('delivers queued gaze before %s ends the browsing phase', async (button, action) => {
    const stopped = deferred<void>()
    owner.stopTracking.mockReturnValue(stopped.promise)
    render(<ParticipationPage />)
    await openFeed()

    fireEvent.click(screen.getByRole('button', { name: button }))
    expect(owner.stopTracking).toHaveBeenCalledTimes(1)
    expect(action).not.toHaveBeenCalled()
    expect(screen.getByRole('status').textContent).toContain('Saving eye-tracking data')
    expect((screen.getByRole('button', { name: 'Continuing...' }) as HTMLButtonElement).disabled).toBe(true)
    expect((screen.getByRole('button', { name: 'Leave study' }) as HTMLButtonElement).disabled).toBe(true)

    await act(async () => { stopped.resolve() })
    expect(action).toHaveBeenCalledTimes(1)
    expect(screen.queryByText('Saving eye-tracking data...')).toBeNull()
  })

  it('offers the live gaze dot switch only in test mode', async () => {
    const { unmount } = render(<ParticipationPage />)
    await openFeed()
    expect(screen.queryByLabelText('Show the live gaze dot (test mode)')).toBeNull()
    unmount()

    owner.snapshot = snapshot({ stage: 'TRACKING', canToggleGazeDot: true })
    render(<ParticipationPage />)
    // The set-up review is remembered for this tab, so the activity introduction comes next.
    fireEvent.click(await screen.findByRole('button', { name: 'Start activity' }))
    fireEvent.click(screen.getByLabelText('Show the live gaze dot (test mode)'))
    expect(owner.setGazeDotVisible).toHaveBeenCalledWith(true)
  })
})
