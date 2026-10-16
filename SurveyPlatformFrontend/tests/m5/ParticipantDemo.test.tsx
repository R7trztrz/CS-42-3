// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom'

const { getParticipation } = vi.hoisted(() => ({ getParticipation: vi.fn() }))
vi.mock('../../src/services/studyApi', () => ({ getParticipation }))
vi.mock('../../src/participant/hooks/useParticipantSession', () => ({
  useParticipantSession: () => ({ stage: 'landing', session: null, sessionToken: null }),
}))

import ParticipantDemo from '../../src/pages/participant/ParticipantDemo'

const study = (title: string) => ({
  title, description: `${title} description`, theme: 'Instagram', content: {},
  eyeTrackingEnabled: true, questionnaireEnabled: false,
})

function open(url: string) {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Link to="/participant-demo?token=study-b">Open another study</Link>
      <Routes>
        <Route path="/participant-demo" element={<ParticipantDemo />} />
        <Route path="/participate/:token" element={<ParticipantDemo />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('demo and published study entry points', () => {
  beforeEach(() => vi.resetAllMocks())
  afterEach(cleanup)

  it('keeps the standalone demo when no study token is supplied', () => {
    open('/participant-demo')
    expect(screen.getByRole('heading', { name: 'How people explore social media content' })).not.toBeNull()
    expect(getParticipation).not.toHaveBeenCalled()
  })

  it.each(['/participant-demo?token=study-a', '/participate/study-a'])(
    'loads the same published study template at %s', async (url) => {
      getParticipation.mockResolvedValue(study('Real study'))
      open(url)
      await screen.findByRole('heading', { name: 'Real study' })
      expect(getParticipation).toHaveBeenCalledWith('study-a')
      expect(screen.getByText('Real study description')).not.toBeNull()
      expect(screen.getByText('Instagram')).not.toBeNull()
      expect(screen.getByRole('list', { name: 'Study steps' })).not.toBeNull()
      expect(screen.queryByText('DMBS-2026-014')).toBeNull()
      expect(screen.queryByText(/research@example.edu/)).toBeNull()
      expect(screen.queryByText('8-10 minutes')).toBeNull()
    },
  )

  it('loads another study when the query token changes', async () => {
    getParticipation.mockResolvedValueOnce(study('Study A')).mockResolvedValueOnce(study('Study B'))
    open('/participant-demo?token=study-a')
    await screen.findByRole('heading', { name: 'Study A' })
    fireEvent.click(screen.getByRole('link', { name: 'Open another study' }))
    await screen.findByRole('heading', { name: 'Study B' })
    expect(getParticipation).toHaveBeenLastCalledWith('study-b')
    expect(screen.queryByText('Study A description')).toBeNull()
  })

  it('shows errors instead of falling back to sample data for an invalid study', async () => {
    getParticipation.mockRejectedValue(new Error('Invalid link'))
    open('/participant-demo?token=invalid')
    await screen.findByRole('heading', { name: 'Study unavailable' })
    expect(screen.queryByText('How people explore social media content')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Begin study' })).toBeNull()
  })

  it('does not treat an empty token as a standalone demo', async () => {
    open('/participant-demo?token=')
    await screen.findByRole('heading', { name: 'Study unavailable' })
    expect(getParticipation).not.toHaveBeenCalled()
  })
})
