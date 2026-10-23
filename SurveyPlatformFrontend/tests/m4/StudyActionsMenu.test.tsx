// @vitest-environment happy-dom

import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { useState } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { StudyResponse } from '../../src/services/studyApi'

const { getStudy, closeStudy, deleteStudy } = vi.hoisted(() => ({ getStudy: vi.fn(), closeStudy: vi.fn(), deleteStudy: vi.fn() }))
vi.mock('../../src/services/studyApi', () => ({ getStudy, closeStudy, deleteStudy }))
import StudyActionsMenu from '../../src/components/studies/StudyActionsMenu'

const study: StudyResponse = { id: 'a', title: 'Campus study', status: 'COLLECTING', description: null, createdAt: '', updatedAt: '', version: 7, eyeTrackingEnabled: false, questionnaireEnabled: false, publishedAt: '', participationUrl: '', closedAt: null }
function Workspace() {
  const [current, setCurrent] = useState<StudyResponse | null>(study)
  const [open, setOpen] = useState(false)
  return current ? <><p>{current.status}</p><StudyActionsMenu study={current} open={open} onOpenChange={setOpen} onChanged={setCurrent} onDeleted={() => setCurrent(null)} /></> : <p>No studies</p>
}
function open() {
  render(<MemoryRouter><Routes><Route path="/" element={<Workspace />} /><Route path="/studies/a/edit" element={<h2>Editor</h2>} /></Routes></MemoryRouter>)
  fireEvent.click(screen.getByRole('button', { name: 'Actions for Campus study' }))
}
afterEach(() => { cleanup(); vi.resetAllMocks() })

describe('study action menu', () => {
  it('opens the editor and dismisses on Escape or outside click', () => {
    open()
    fireEvent.keyDown(screen.getByRole('menu'), { key: 'Escape' })
    expect(screen.queryByRole('menu')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Actions for Campus study' }))
    fireEvent.pointerDown(document.body)
    expect(screen.queryByRole('menu')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Actions for Campus study' }))
    fireEvent.click(screen.getByRole('menuitem', { name: 'Open study' }))
    expect(screen.getByRole('heading', { name: 'Editor' })).toBeTruthy()
  })

  it('gets the latest study version before closing, and updates status on success', async () => {
    getStudy.mockResolvedValue({ ...study, version: 9 })
    closeStudy.mockResolvedValue({ ...study, status: 'CLOSED', version: 10 })
    open()
    fireEvent.click(screen.getByRole('menuitem', { name: 'Close study' }))
    await screen.findByText('Campus study', { selector: 'p' })
    expect(closeStudy).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Confirm closure' }))
    await screen.findByText('CLOSED')
    expect(closeStudy).toHaveBeenCalledWith('a', 9)
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('requires the exact title before deletion and removes the item only after success', async () => {
    getStudy.mockResolvedValue(study)
    let resolve!: () => void
    deleteStudy.mockReturnValue(new Promise<void>(done => { resolve = done }))
    open()
    fireEvent.click(screen.getByRole('menuitem', { name: 'Delete study' }))
    const input = await screen.findByRole('textbox', { name: 'Type the study title to confirm' })
    expect(screen.getByRole('button', { name: 'Delete permanently' })).toHaveProperty('disabled', true)
    fireEvent.change(input, { target: { value: 'Campus study' } })
    fireEvent.click(screen.getByRole('button', { name: 'Delete permanently' }))
    expect(deleteStudy).toHaveBeenCalledWith('a', 7)
    expect(screen.getByRole('button', { name: 'Deleting...' })).toHaveProperty('disabled', true)
    expect(screen.queryByText('No studies')).toBeNull()
    await act(async () => resolve())
    expect(screen.getByText('No studies')).toBeTruthy()
  })

  it('keeps a failed deletion visible and requires reconfirmation with the refreshed version', async () => {
    getStudy.mockResolvedValueOnce(study).mockResolvedValueOnce({ ...study, version: 10 })
    deleteStudy.mockRejectedValueOnce({ response: { data: { code: 'STUDY_VERSION_CONFLICT' } } }).mockResolvedValueOnce(undefined)
    open()
    fireEvent.click(screen.getByRole('menuitem', { name: 'Delete study' }))
    const input = await screen.findByRole('textbox')
    fireEvent.change(input, { target: { value: 'Campus study' } })
    fireEvent.click(screen.getByRole('button', { name: 'Delete permanently' }))
    await screen.findByRole('alert')
    expect(input).toHaveProperty('value', '')
    expect(deleteStudy).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Delete permanently' })).toHaveProperty('disabled', true)
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Campus study' } })
    fireEvent.click(screen.getByRole('button', { name: 'Delete permanently' }))
    await waitFor(() => expect(deleteStudy).toHaveBeenLastCalledWith('a', 10))
    await screen.findByText('No studies')
  })

  it('does not call deletion when the confirmation is cancelled', async () => {
    getStudy.mockResolvedValue(study)
    open()
    fireEvent.click(screen.getByRole('menuitem', { name: 'Delete study' }))
    await screen.findByRole('textbox')
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(deleteStudy).not.toHaveBeenCalled()
  })
})
