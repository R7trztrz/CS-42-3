// @vitest-environment happy-dom

import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { StudyResponse } from '../../src/services/studyApi'

const { closeStudy, getStudy } = vi.hoisted(() => ({ closeStudy: vi.fn(), getStudy: vi.fn() }))
vi.mock('../../src/services/studyApi', () => ({ closeStudy, getStudy }))
import StudyClosureControl from '../../src/components/studies/StudyClosureControl'

const study: StudyResponse = {
  id: 'study-a', title: 'Campus study', description: null, status: 'COLLECTING',
  createdAt: '', updatedAt: '', version: 3, eyeTrackingEnabled: false,
  questionnaireEnabled: true, publishedAt: '2026-10-23T00:00:00Z',
  participationUrl: '/participate/token', closedAt: null,
}
const closed: StudyResponse = { ...study, status: 'CLOSED', version: 4, closedAt: '2026-10-23T01:00:00Z' }
function Workspace() {
  const [current, setCurrent] = useState(study)
  return <><span>{current.status}</span><StudyClosureControl study={current} onStudyChange={setCurrent} /></>
}
function confirm() {
  fireEvent.click(screen.getByRole('button', { name: 'Close study' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirm closure' }))
}
afterEach(() => { cleanup(); vi.resetAllMocks() })

describe('study closure', () => {
  it('hides the operation for drafts and already closed studies', () => {
    const { rerender } = render(<StudyClosureControl study={{ ...study, status: 'DRAFT' }} onStudyChange={vi.fn()} />)
    expect(screen.queryByRole('button')).toBeNull()
    rerender(<StudyClosureControl study={closed} onStudyChange={vi.fn()} />)
    expect(screen.queryByRole('button')).toBeNull()
    expect(screen.getByRole('status').textContent).toContain('Closed')
  })

  it('requires confirmation and leaves the study unchanged on cancellation', () => {
    render(<Workspace />)
    fireEvent.click(screen.getByRole('button', { name: 'Close study' }))
    expect(screen.getByRole('dialog').textContent).toContain('This cannot be undone.')
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(closeStudy).not.toHaveBeenCalled()
    expect(screen.getByText('COLLECTING')).toBeTruthy()
  })

  it('sends the study version and displays the returned closed state', async () => {
    closeStudy.mockResolvedValue(closed)
    render(<Workspace />)
    confirm()
    await screen.findByText('CLOSED')
    expect(closeStudy).toHaveBeenCalledWith('study-a', 3)
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Close study' })).toBeNull()
    expect(screen.getByRole('status').querySelector('time')?.getAttribute('datetime')).toBe(closed.closedAt)
  })

  it('prevents duplicate requests and keeps status unchanged when closing fails', async () => {
    let reject!: (error: Error) => void
    closeStudy.mockReturnValue(new Promise((_, fail) => { reject = fail }))
    render(<Workspace />)
    confirm()
    fireEvent.click(screen.getByRole('button', { name: 'Closing...' }))
    expect(closeStudy).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveProperty('disabled', true)
    await act(async () => reject(new Error('Network failure')))
    expect(screen.getByRole('alert').textContent).toContain('could not be closed')
    expect(screen.getByText('COLLECTING')).toBeTruthy()
  })

  it('refreshes a stale version and requires another explicit confirmation', async () => {
    closeStudy.mockRejectedValueOnce({ response: { data: { code: 'STUDY_VERSION_CONFLICT' } } }).mockResolvedValueOnce(closed)
    getStudy.mockResolvedValue({ ...study, version: 8 })
    render(<Workspace />)
    confirm()
    await screen.findByRole('alert')
    expect(getStudy).toHaveBeenCalledWith('study-a')
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(closeStudy).toHaveBeenCalledTimes(1)
    confirm()
    await screen.findByText('CLOSED')
    expect(closeStudy).toHaveBeenLastCalledWith('study-a', 8)
  })
})
