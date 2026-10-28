// @vitest-environment happy-dom
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ExportJob } from '../../src/services/exportApi'

const { getStudy, createStudyExport, createSessionExport, getStudyExport, downloadStudyExport } = vi.hoisted(() => ({ getStudy: vi.fn(), createStudyExport: vi.fn(), createSessionExport: vi.fn(), getStudyExport: vi.fn(), downloadStudyExport: vi.fn() }))
vi.mock('../../src/services/studyApi', () => ({ getStudy }))
vi.mock('../../src/services/exportApi', () => ({ createStudyExport, createSessionExport, getStudyExport, downloadStudyExport }))
import StudyExportDialog from '../../src/components/studies/StudyExportDialog'
import StudyDataExportButton from '../../src/components/studies/StudyDataExportButton'

const job: ExportJob = { id: 'export-1', studyId: 'study-1', sessionId: null, scope: 'STUDY', dataTypes: ['QUESTIONNAIRE', 'GAZE', 'BEHAVIOR'], status: 'PENDING', createdAt: '', startedAt: null, snapshotAt: null, completedAt: null, expiresAt: null, errorCode: null, errorMessage: null, failedSessionId: null }
const props = { studyId: 'study-1', studyTitle: 'Campus study', status: 'COLLECTING' as const, open: true, onClose: vi.fn() }
beforeEach(() => {
  getStudy.mockResolvedValue({ status: 'COLLECTING' })
  createStudyExport.mockResolvedValue(job)
  getStudyExport.mockResolvedValue({ ...job, status: 'COMPLETED' })
  downloadStudyExport.mockResolvedValue(undefined)
})
afterEach(() => { cleanup(); vi.resetAllMocks(); vi.useRealTimers() })

describe('Study export dialog', () => {
  it.each(['session-a', 'session-b'])('exports only the selected session %s', async sessionId => {
    createSessionExport.mockResolvedValue({ ...job, scope: 'SESSION', sessionId })
    getStudyExport.mockResolvedValue({ ...job, scope: 'SESSION', sessionId, status: 'COMPLETED' })
    render(<StudyDataExportButton studyId="study-1" studyTitle="Campus study" status="CLOSED" session={{ sessionId, participantId: 'anonymous-person' }} />)
    fireEvent.click(screen.getByRole('button', { name: `Export data for session ${sessionId}` }))
    expect(screen.getByText('Scope: this participant session only.')).toBeTruthy()
    expect(screen.getByText(`Session ID: ${sessionId}`)).toBeTruthy()
    expect(screen.getByText('Participant ID: anonymous-person')).toBeTruthy()
    expect(screen.queryByText(/data_availability.csv/)).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Generate export' }))
    await screen.findByText('Ready to download')
    expect(createSessionExport).toHaveBeenCalledWith('study-1', sessionId, ['QUESTIONNAIRE', 'GAZE', 'BEHAVIOR'])
    expect(createStudyExport).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Download ZIP' }))
    await screen.findByText(/Download requested/)
    expect(downloadStudyExport).toHaveBeenCalledWith('study-1', 'export-1', 'SESSION')
  })

  it('opens from the detail entry without creating a task automatically', () => {
    render(<StudyDataExportButton studyId="study-1" studyTitle="Campus study" status="COLLECTING" />)
    fireEvent.click(screen.getByRole('button', { name: 'Export data' }))
    expect(screen.getByRole('dialog')).toBeTruthy()
    expect(screen.getByText('Campus study')).toBeTruthy()
    expect(createStudyExport).not.toHaveBeenCalled()
  })

  it('requires a category and sends only the selected types, then downloads the completed task', async () => {
    render(<StudyExportDialog {...props} />)
    screen.getAllByRole('checkbox').forEach(checkbox => fireEvent.click(checkbox))
    expect(screen.getByRole('button', { name: 'Generate export' })).toHaveProperty('disabled', true)
    fireEvent.click(screen.getByRole('checkbox', { name: /Questionnaire/ }))
    fireEvent.click(screen.getByRole('button', { name: 'Generate export' }))
    await screen.findByText('Ready to download')
    expect(createStudyExport).toHaveBeenCalledWith('study-1', ['QUESTIONNAIRE'])
    fireEvent.click(screen.getByRole('button', { name: 'Download ZIP' }))
    await screen.findByText(/Download requested/)
    expect(downloadStudyExport).toHaveBeenCalledWith('study-1', 'export-1')
  })

  it('prevents duplicate creation while the initial request is pending', async () => {
    let resolve!: (value: ExportJob) => void
    createStudyExport.mockReturnValue(new Promise<ExportJob>(done => { resolve = done }))
    render(<StudyExportDialog {...props} />)
    fireEvent.click(screen.getByRole('button', { name: 'Generate export' }))
    await waitFor(() => expect(createStudyExport).toHaveBeenCalledTimes(1))
    expect(screen.getByRole('button', { name: 'Creating export...' })).toHaveProperty('disabled', true)
    fireEvent.click(screen.getByRole('button', { name: 'Creating export...' }))
    expect(createStudyExport).toHaveBeenCalledTimes(1)
    await act(async () => resolve(job))
    await screen.findByText('Ready to download')
  })

  it('checks the latest lifecycle and refuses a study that is now draft', async () => {
    getStudy.mockResolvedValue({ status: 'DRAFT' })
    render(<StudyExportDialog {...props} />)
    fireEvent.click(screen.getByRole('button', { name: 'Generate export' }))
    await screen.findByText('Publish this study before exporting data.')
    expect(createStudyExport).not.toHaveBeenCalled()
  })

  it('polls queued tasks and stops after completion', async () => {
    vi.useFakeTimers()
    getStudyExport.mockResolvedValueOnce({ ...job, status: 'RUNNING' }).mockResolvedValue({ ...job, status: 'COMPLETED' })
    render(<StudyExportDialog {...props} />)
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Generate export' })) })
    expect(screen.getByText('Generating')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Download ZIP' })).toBeNull()
    await act(async () => { await vi.advanceTimersByTimeAsync(2000) })
    expect(screen.getByText('Ready to download')).toBeTruthy()
    await act(async () => { await vi.advanceTimersByTimeAsync(6000) })
    expect(getStudyExport).toHaveBeenCalledTimes(2)
  })

  it('retries status checks without creating another task and preserves the task on reopen', async () => {
    getStudyExport.mockRejectedValueOnce(new Error('Offline')).mockResolvedValue({ ...job, status: 'COMPLETED' })
    const view = render(<StudyExportDialog {...props} />)
    fireEvent.click(screen.getByRole('button', { name: 'Generate export' }))
    await screen.findByRole('button', { name: 'Retry status check' })
    view.rerender(<StudyExportDialog {...props} open={false} />)
    expect(screen.queryByRole('dialog')).toBeNull()
    view.rerender(<StudyExportDialog {...props} />)
    await screen.findByText('Ready to download')
    expect(createStudyExport).toHaveBeenCalledTimes(1)
  })

  it('shows generation failures and permits a new task', async () => {
    getStudyExport.mockResolvedValue({ ...job, status: 'FAILED', errorMessage: 'Invalid saved data', failedSessionId: 'session-9' })
    render(<StudyExportDialog {...props} />)
    fireEvent.click(screen.getByRole('button', { name: 'Generate export' }))
    await screen.findByText(/Invalid saved data/)
    expect(screen.getByText(/Session: session-9/)).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Generate new export' })).toHaveProperty('disabled', false)
    expect(screen.queryByRole('button', { name: 'Download ZIP' })).toBeNull()
  })

  it('handles expired download files and offers regeneration', async () => {
    downloadStudyExport.mockRejectedValue({ isAxiosError: true, response: { status: 410 } })
    render(<StudyExportDialog {...props} />)
    fireEvent.click(screen.getByRole('button', { name: 'Generate export' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Download ZIP' }))
    await screen.findByText('Export expired')
    expect(screen.queryByRole('button', { name: 'Download ZIP' })).toBeNull()
  })
})
