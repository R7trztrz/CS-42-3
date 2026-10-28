// @vitest-environment happy-dom
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { StudyParticipationResponse, StudyResponse } from '../../src/services/studyApi'

const { get } = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('../../src/services/api', () => ({ default: { get } }))
import StudyParticipationPage from '../../src/pages/researcher/StudyParticipationPage'

const study: StudyResponse = { id: 'study-1', title: 'Database study', description: 'Saved study description', status: 'COLLECTING', createdAt: '', updatedAt: '', version: 1, eyeTrackingEnabled: true, questionnaireEnabled: true, publishedAt: '', participationUrl: '', closedAt: null }
function records(page = 0): StudyParticipationResponse {
  return {
    summary: { total: 12, inProgress: 2, completed: 9, abandoned: 1 },
    sessions: { content: [{ sessionId: `real-session-${page}`, anonymousParticipantId: 'participant-from-database', status: 'COMPLETED', enteredAt: '2026-10-27T01:00:00Z', completedAt: '2026-10-27T01:15:00Z' }], page, size: 10, totalElements: 12, totalPages: 2 },
  }
}
function setup(status: StudyResponse['status'] = 'COLLECTING') {
  get.mockImplementation(async (url: string, config?: { params: { page: number } }) => ({ data: url.endsWith('/participation') ? records(config?.params.page) : { ...study, status } }))
}
function mount() {
  return render(<MemoryRouter initialEntries={['/studies/study-1/participation']}><Routes><Route path="/studies/:studyId/participation" element={<StudyParticipationPage />} /></Routes></MemoryRouter>)
}
afterEach(() => { cleanup(); vi.resetAllMocks() })

describe('real study participation page', () => {
  it('opens an export for each row with its own session identity, regardless of session completion status', async () => {
    get.mockImplementation(async (url: string) => {
      const response = records()
      response.sessions.content = ['IN_PROGRESS', 'COMPLETED', 'ABANDONED'].map((status, index) => ({ ...response.sessions.content[0], sessionId: `session-${index}`, anonymousParticipantId: `person-${index}`, status: status as 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED' }))
      return { data: url.endsWith('/participation') ? response : { ...study, status: 'CLOSED' } }
    })
    mount()
    await screen.findByText('session-0')
    for (const index of [0, 1, 2]) {
      const button = screen.getByRole('button', { name: `Export data for session session-${index}` })
      expect(button).toHaveProperty('disabled', false)
      fireEvent.click(button)
      expect(screen.getByText(`Session ID: session-${index}`)).toBeTruthy()
      expect(screen.getByText(`Participant ID: person-${index}`)).toBeTruthy()
      fireEvent.click(screen.getByRole('button', { name: 'Close' }))
      expect(screen.queryByRole('dialog')).toBeNull()
    }
  })

  it('reads study details and server totals, and forwards pagination, sorting and refresh requests', async () => {
    setup()
    mount()
    await screen.findByText('real-session-0')
    expect(screen.getByText('Database study')).toBeTruthy()
    expect(screen.getByText('Saved study description')).toBeTruthy()
    expect(screen.getByLabelText('Participation summary').textContent).toContain('12')
    expect(screen.queryByText('Completed at')).toBeNull()
    expect(screen.getByRole('button', { name: 'Export data for session real-session-0' })).toBeTruthy()
    expect(get).toHaveBeenCalledWith('/api/studies/study-1/participation', { params: { page: 0, size: 10, sortBy: 'enteredAt', direction: 'desc' }, signal: expect.any(AbortSignal) })
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }))
    await screen.findByText('real-session-1')
    fireEvent.change(screen.getByLabelText('Sort by'), { target: { value: 'status' } })
    await screen.findByText('real-session-0')
    fireEvent.click(screen.getByRole('button', { name: 'Sort ascending' }))
    await waitFor(() => expect(get).toHaveBeenLastCalledWith('/api/studies/study-1/participation', expect.objectContaining({ params: { page: 0, size: 10, sortBy: 'status', direction: 'asc' } })))
    await screen.findByText('real-session-0')
    fireEvent.change(screen.getByLabelText('Rows per page'), { target: { value: '20' } })
    await waitFor(() => expect(get).toHaveBeenLastCalledWith('/api/studies/study-1/participation', expect.objectContaining({ params: { page: 0, size: 20, sortBy: 'status', direction: 'asc' } })))
    await screen.findByText('real-session-0')
    const calls = get.mock.calls.length
    fireEvent.click(screen.getByRole('button', { name: 'Refresh records' }))
    await waitFor(() => expect(get.mock.calls.length).toBe(calls + 2))
  })

  it('does not request records when a draft URL is opened directly', async () => {
    setup('DRAFT'); mount()
    await screen.findByText(/Publish this study before/)
    expect(get).toHaveBeenCalledTimes(1)
    expect(screen.queryByLabelText('Participation summary')).toBeNull()
  })

  it('allows closed studies and renders empty results without invalid pagination', async () => {
    get.mockImplementation(async (url: string) => ({ data: url.endsWith('/participation') ? { summary: { total: 0, inProgress: 0, completed: 0, abandoned: 0 }, sessions: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } } : { ...study, status: 'CLOSED' } }))
    mount()
    await screen.findByText('No participant sessions yet.')
    expect(screen.getByText('Closed')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Next page' })).toHaveProperty('disabled', true)
    expect(screen.getByRole('button', { name: 'Previous page' })).toHaveProperty('disabled', true)
  })

  it('offers retry after a network failure', async () => {
    get.mockRejectedValueOnce(new Error('Offline')); mount()
    await screen.findByRole('alert')
    setup()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await screen.findByText('real-session-0')
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('does not show participant data for a missing or foreign study', async () => {
    get.mockRejectedValue({ isAxiosError: true, response: { status: 404 } }); mount()
    expect((await screen.findByRole('alert')).textContent).toContain('not available to your account')
    expect(get).toHaveBeenCalledTimes(1)
  })

  it('ignores a pending response after the page is unmounted', async () => {
    let finish!: (value: { data: StudyResponse }) => void
    get.mockReturnValue(new Promise(resolve => { finish = resolve }))
    const view = mount()
    view.unmount()
    await act(async () => finish({ data: study }))
    expect(get).toHaveBeenCalledTimes(1)
  })
})
