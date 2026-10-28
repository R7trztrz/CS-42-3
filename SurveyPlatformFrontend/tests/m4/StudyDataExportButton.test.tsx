// @vitest-environment happy-dom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import StudyDataExportButton from '../../src/components/studies/StudyDataExportButton'

afterEach(() => { cleanup(); vi.unstubAllGlobals() })

describe.each([false, true])('export placeholder (menu item: %s)', menuItem => {
  it.each(['DRAFT', 'COLLECTING', 'CLOSED'] as const)('permits only published studies: %s', status => {
    const alert = vi.fn()
    vi.stubGlobal('alert', alert)
    const onActivate = vi.fn()
    render(<StudyDataExportButton status={status} menuItem={menuItem} onActivate={onActivate} />)
    const button = screen.getByRole(menuItem ? 'menuitem' : 'button', { name: 'Export data' })
    expect(button).toHaveProperty('disabled', status === 'DRAFT')
    fireEvent.click(button)
    expect(onActivate).toHaveBeenCalledTimes(status === 'DRAFT' ? 0 : 1)
    expect(alert).toHaveBeenCalledTimes(status === 'DRAFT' ? 0 : 1)
    if (status !== 'DRAFT') expect(alert).toHaveBeenCalledWith(expect.stringContaining('No file has been generated.'))
  })
})
