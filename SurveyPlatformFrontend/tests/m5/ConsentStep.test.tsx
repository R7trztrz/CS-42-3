// @vitest-environment happy-dom

import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'

import ConsentStep from '../../src/participant/components/ConsentStep'
import { makeConsentDocument } from './fixtures'

afterEach(() => {
  cleanup()
})

describe('ConsentStep', () => {
  it('warns that the placeholder consent text is not approved for production', () => {
    render(
      <ConsentStep
        consentDocument={makeConsentDocument({ approvedForProduction: false })}
        isPending={false}
        errorMessage={null}
        onDecide={vi.fn()}
      />,
    )

    expect(screen.getByRole('note').textContent).toMatch(/not been approved for/i)
  })

  it('does not show the placeholder warning once a document is approved', () => {
    render(
      <ConsentStep
        consentDocument={makeConsentDocument({ approvedForProduction: true })}
        isPending={false}
        errorMessage={null}
        onDecide={vi.fn()}
      />,
    )

    expect(screen.queryByRole('note')).toBeNull()
  })

  it('calls onDecide(true) for accept and onDecide(false) for decline', () => {
    const onDecide = vi.fn().mockResolvedValue(true)
    render(
      <ConsentStep
        consentDocument={makeConsentDocument()}
        isPending={false}
        errorMessage={null}
        onDecide={onDecide}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: /i agree to participate/i }))
    expect(onDecide).toHaveBeenCalledWith(true)

    fireEvent.click(screen.getByRole('button', { name: /i do not agree/i }))
    expect(onDecide).toHaveBeenCalledWith(false)
  })

  it('disables both buttons while a decision is pending', () => {
    render(
      <ConsentStep
        consentDocument={makeConsentDocument()}
        isPending={true}
        errorMessage={null}
        onDecide={vi.fn()}
      />,
    )

    expect(
      (screen.getByRole('button', { name: /i agree to participate/i }) as HTMLButtonElement)
        .disabled,
    ).toBe(true)
    expect(
      (screen.getByRole('button', { name: /i do not agree/i }) as HTMLButtonElement).disabled,
    ).toBe(true)
  })

  it('surfaces an action error message', () => {
    render(
      <ConsentStep
        consentDocument={makeConsentDocument()}
        isPending={false}
        errorMessage="Something went wrong. Please try again."
        onDecide={vi.fn()}
      />,
    )

    expect(screen.getByRole('alert').textContent).toBe('Something went wrong. Please try again.')
  })
})
