// @vitest-environment happy-dom

import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'

import FinishedStep from '../../src/participant/components/FinishedStep'

afterEach(() => {
  cleanup()
})

describe('FinishedStep', () => {
  it('thanks a participant who completed the study', () => {
    render(<FinishedStep status="COMPLETED" abandonmentReason={null} />)
    expect(screen.getByText('Thank you for participating')).toBeTruthy()
  })

  it('shows a decline-specific message for CONSENT_DECLINED', () => {
    render(<FinishedStep status="ABANDONED" abandonmentReason="CONSENT_DECLINED" />)
    expect(screen.getByText(/chose not to take part/i)).toBeTruthy()
  })

  it('shows an exit-specific message for PARTICIPANT_EXIT', () => {
    render(<FinishedStep status="ABANDONED" abandonmentReason="PARTICIPANT_EXIT" />)
    expect(screen.getByText('You have left the study')).toBeTruthy()
  })

  it('shows a timeout-specific message for INACTIVITY_TIMEOUT', () => {
    render(<FinishedStep status="ABANDONED" abandonmentReason="INACTIVITY_TIMEOUT" />)
    expect(screen.getByText('Session timed out')).toBeTruthy()
  })

  it('shows a closed-study message for STUDY_CLOSED', () => {
    render(<FinishedStep status="ABANDONED" abandonmentReason="STUDY_CLOSED" />)
    expect(screen.getByText('This study is closed')).toBeTruthy()
  })
})
