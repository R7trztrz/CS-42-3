// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ParticipationOverviewDemo from '../../src/pages/researcher/ParticipationOverviewDemo'

afterEach(() => { cleanup(); vi.restoreAllMocks() })

describe('standalone participation overview', () => {
  it('shows mock statistics without account access or network requests', () => {
    const fetch = vi.spyOn(globalThis, 'fetch')
    const accountRead = vi.spyOn(Storage.prototype, 'getItem')
    render(<ParticipationOverviewDemo />)
    const summary = within(screen.getByRole('region', { name: 'Participation summary' }))
    for (const value of ['36', '8', '24', '4']) expect(summary.getByText(value)).toBeTruthy()
    expect(screen.getAllByRole('row')).toHaveLength(11)
    expect(screen.queryByRole('button', { name: 'Logout' })).toBeNull()
    expect(fetch).not.toHaveBeenCalled()
    expect(accountRead).not.toHaveBeenCalled()
  })

  it('switches to a closed study with completed and abandoned sessions only', () => {
    render(<ParticipationOverviewDemo />)
    fireEvent.click(screen.getByRole('radio', { name: 'Closed' }))
    const summary = within(screen.getByRole('region', { name: 'Participation summary' }))
    for (const value of ['36', '0', '24', '12']) expect(summary.getByText(value)).toBeTruthy()
    expect(within(screen.getByRole('table')).queryByText('In progress')).toBeNull()
  })

  it('paginates and sorts locally, with counts independent of page size', () => {
    render(<ParticipationOverviewDemo />)
    expect(screen.getByRole('button', { name: 'Previous page' })).toHaveProperty('disabled', true)
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }))
    expect(screen.getByText('11-20')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Rows per page'), { target: { value: '20' } })
    expect(screen.getAllByRole('row')).toHaveLength(21)
    expect(screen.getByText('1-20')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Sort by'), { target: { value: 'status' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sort ascending' }))
    expect(screen.getAllByRole('row')[1].textContent).toContain('In progress')
    fireEvent.click(screen.getByRole('button', { name: 'Refresh records' }))
    expect(screen.getByRole('status').textContent).toBe('Records refreshed.')
    expect(within(screen.getByRole('region', { name: 'Participation summary' })).getByText('36')).toBeTruthy()
  })
})
