// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'

const { listStudies } = vi.hoisted(() => ({ listStudies: vi.fn() }))
vi.mock('../../src/services/studyApi', () => ({ listStudies, createStudy: vi.fn() }))
import ResearcherDashboard from '../../src/pages/researcher/ResearcherDashboard'
import StudyList from '../../src/pages/researcher/StudyList'

afterEach(() => { cleanup(); vi.resetAllMocks() })

describe.each([
  ['dashboard', ResearcherDashboard], ['study list', StudyList],
] as const)('%s closed studies', (_, Page) => {
  it('keeps closed studies visible without an open action or row navigation', async () => {
    listStudies.mockResolvedValue({ content: ['DRAFT', 'COLLECTING', 'CLOSED'].map(status => ({
      id: status, title: `${status} study`, status, createdAt: '2026-10-23T00:00:00Z', updatedAt: '2026-10-23T00:00:00Z',
    })), page: 0, size: 100, totalElements: 3, totalPages: 1 })
    render(<MemoryRouter><Routes>
      <Route path="/" element={<Page />} />
      <Route path="/studies/:id/edit" element={<h2>Study editor</h2>} />
    </Routes></MemoryRouter>)
    const closedTitle = await screen.findByText('CLOSED study')
    const row = closedTitle.closest('tr')!
    expect(within(row).getByText('CLOSED')).toBeTruthy()
    expect(within(row).queryByText('Open study')).toBeNull()
    expect(within(row).queryByRole('link')).toBeNull()
    expect(row.getAttribute('role')).toBeNull()
    expect(row.getAttribute('tabindex')).toBeNull()
    fireEvent.click(row)
    fireEvent.keyDown(row, { key: 'Enter' })
    fireEvent.keyDown(row, { key: ' ' })
    expect(screen.queryByRole('heading', { name: 'Study editor' })).toBeNull()
    fireEvent.click(within(row).getByRole('button', { name: 'Actions for CLOSED study' }))
    expect(screen.getByRole('menuitem', { name: 'Open study' })).toHaveProperty('disabled', true)
    expect(screen.getByRole('menuitem', { name: 'Close study' })).toHaveProperty('disabled', true)
    expect(screen.getByRole('menuitem', { name: 'Delete study' })).toHaveProperty('disabled', false)
    expect(screen.queryByRole('heading', { name: 'Study editor' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Actions for DRAFT study' }))
    expect(screen.getAllByRole('menu')).toHaveLength(1)
    expect(screen.getByRole('menuitem', { name: 'Open study' })).toHaveProperty('disabled', false)
    expect(screen.getByRole('menuitem', { name: 'Close study' })).toHaveProperty('disabled', true)
    fireEvent.click(screen.getByRole('button', { name: 'Actions for COLLECTING study' }))
    expect(screen.getAllByRole('menu')).toHaveLength(1)
    expect(screen.getByRole('menuitem', { name: 'Open study' })).toHaveProperty('disabled', false)
    expect(screen.getByRole('menuitem', { name: 'Close study' })).toHaveProperty('disabled', false)
  })
})
