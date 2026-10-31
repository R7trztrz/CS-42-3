// @vitest-environment happy-dom
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { StudyParticipantDetail } from '../../src/services/studyApi'

const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))
vi.mock('../../src/services/api', () => ({ default: { get, post } }))
import ParticipantSessionDetails from '../../src/pages/researcher/ParticipantSessionDetails'

const participant: StudyParticipantDetail = {
  studyId: 'study-1', sessionId: 'session-1', anonymousParticipantId: 'anonymous-1', status: 'ABANDONED', phase: 'FINISHED', abandonmentReason: 'STUDY_CLOSED',
  enteredAt: '2026-10-30T01:00:00Z', consentedAt: '2026-10-30T01:01:00Z', calibrationCompletedAt: '2026-10-30T01:02:00Z', browsingCompletedAt: null, completedAt: null, abandonedAt: '2026-10-30T01:03:00Z', lastActivityAt: '2026-10-30T01:02:30Z',
  deviceInfo: { browser: 'Test browser', browserVersion: '100', os: 'Windows', screenWidth: 1920, screenHeight: 1080, timezone: 'Australia/Sydney' },
  eyeTracking: { enabled: true, calibrationState: 'RECORDED', calibrationAttemptCount: 2, completedAttemptId: 'attempt-1', trackingAvailable: false, unavailableReason: 'PERMISSION_DENIED', quality: 'NOT_MEASURED', qualityBasis: 'NONE', lowQualityReasons: ['ENGINE_UNAVAILABLE'] },
}
function setup(detail = participant, studyStatus = 'CLOSED') {
  get.mockImplementation(async (url: string) => ({ data: url.endsWith('/participants/session-1') ? detail : { id: 'study-1', title: 'Real study title', status: studyStatus } }))
}
function mount() {
  return render(<MemoryRouter initialEntries={['/studies/study-1/participants/session-1']}><Routes><Route path="/studies/:studyId/participants/:sessionId" element={<ParticipantSessionDetails />} /></Routes></MemoryRouter>)
}
afterEach(() => { cleanup(); vi.resetAllMocks() })

describe('participant session details', () => {
  it('loads persisted session fields, time zone, device and final calibration metadata', async () => {
    setup(); mount()
    await screen.findByText('anonymous-1')
    expect(screen.getByText('Real study title')).toBeTruthy()
    expect(screen.getByText('Finished')).toBeTruthy()
    expect(screen.getByText('Study closed')).toBeTruthy()
    expect(screen.getByText('Camera permission denied')).toBeTruthy()
    expect(screen.getByText('Final attempt recorded')).toBeTruthy()
    expect(screen.getByText('1920 px')).toBeTruthy()
    expect(screen.getByText('Browser').nextElementSibling?.textContent).toBe('Test browser')
    expect(screen.getByText('Session completed').nextElementSibling?.textContent).toBe('—')
    expect(screen.getByText('Entered study').nextElementSibling?.querySelector('time')?.dateTime).toBe(participant.enteredAt)
    expect(screen.getByRole('link', { name: 'Back to participants' }).getAttribute('href')).toBe('/studies/study-1/participation')
    expect(get).toHaveBeenCalledWith('/api/studies/study-1/participants/session-1', { signal: expect.any(AbortSignal) })
  })

  it.each([
    ['Export questionnaire', 'QUESTIONNAIRE'], ['Export eye tracking', 'GAZE'], ['Export interactions', 'BEHAVIOR'],
  ])('opens %s with only its category preselected and the correct session target', async (label, category) => {
    setup(); mount()
    post.mockResolvedValue({ data: { id: 'export-1', status: 'COMPLETED', dataTypes: [category], scope: 'SESSION', sessionId: 'session-1' } })
    fireEvent.click(await screen.findByRole('button', { name: `${label} for session session-1` }))
    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByText('Session ID: session-1')).toBeTruthy()
    expect(within(dialog).getByText('Participant ID: anonymous-1')).toBeTruthy()
    expect(within(dialog).getAllByRole('checkbox').filter(input => (input as HTMLInputElement).checked)).toHaveLength(1)
    fireEvent.click(within(dialog).getByRole('button', { name: 'Generate export' }))
    await screen.findByText('Ready to download')
    expect(post).toHaveBeenCalledWith('/api/studies/study-1/participants/session-1/exports', { dataTypes: [category] })
  })

  it('does not invent device, consent or tracker availability data', async () => {
    setup({ ...participant, deviceInfo: null, consentedAt: null, eyeTracking: { ...participant.eyeTracking, enabled: false, calibrationState: 'DISABLED', calibrationAttemptCount: 0, completedAttemptId: null, trackingAvailable: null, unavailableReason: null, quality: null, qualityBasis: null, lowQualityReasons: [] } })
    mount()
    await screen.findByText('No device information was recorded for this session.')
    expect(screen.getByText('Consent given').nextElementSibling?.textContent).toBe('—')
    expect(screen.getByText('Recorded tracker availability').nextElementSibling?.textContent).toBe('Not recorded')
    expect(screen.getByText('Eye tracking is disabled for this study.')).toBeTruthy()
    expect(screen.getByText(/do not prove that gaze events were saved/)).toBeTruthy()
  })

  it('blocks draft studies without requesting participant details', async () => {
    setup(participant, 'DRAFT'); mount()
    await screen.findByText('Session details are available only for collecting or closed studies.')
    expect(get).toHaveBeenCalledTimes(1)
    expect(screen.queryByText('anonymous-1')).toBeNull()
  })

  it.each([401, 403, 404])('handles unavailable access (%s) without exposing session information', async status => {
    get.mockRejectedValue({ isAxiosError: true, response: { status } }); mount()
    await screen.findByRole('alert')
    expect(screen.queryByText('anonymous-1')).toBeNull()
    expect(screen.queryByRole('button', { name: /Export interactions/ })).toBeNull()
  })

  it('retries a failed request and refreshes persisted phase changes', async () => {
    get.mockRejectedValueOnce(new Error('Offline')); mount()
    await screen.findByRole('alert')
    setup(); fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await screen.findByText('anonymous-1')
    setup({ ...participant, status: 'IN_PROGRESS', phase: 'QUESTIONNAIRE', abandonmentReason: null }, 'COLLECTING')
    fireEvent.click(screen.getByRole('button', { name: 'Refresh details' }))
    await screen.findByText('Final questionnaire')
    expect(screen.getByText('In progress')).toBeTruthy()
  })

  it('ignores a response after leaving the page', async () => {
    let finish!: (value: unknown) => void
    get.mockReturnValue(new Promise(resolve => { finish = resolve }))
    const view = mount(); view.unmount()
    await act(async () => finish({ data: { status: 'COLLECTING' } }))
    expect(get).toHaveBeenCalledTimes(1)
  })
})
