// @vitest-environment happy-dom

import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

const { get, put } = vi.hoisted(() => ({ get: vi.fn(), put: vi.fn() }))
vi.mock('../../src/participant/api/participantHttpClient', () => ({ default: { get, put } }))

import QuestionnaireStep from '../../src/participant/components/QuestionnaireStep'
import { makeQuestion } from './fixtures'

afterEach(() => { cleanup(); vi.resetAllMocks() })

describe('published study questionnaire', () => {
  it('keeps the first selection when a loaded question becomes interactive', async () => {
    get.mockResolvedValue({ data: { currentQuestion: makeQuestion(), readyToSubmit: false } })
    let clicked = false
    // Observe the DOM commit instead of depending on a timer to flush passive effects first.
    const observer = new MutationObserver(() => {
      const radio = screen.queryByLabelText('Yes') as HTMLInputElement | null
      if (!radio || radio.closest('fieldset')?.disabled || clicked) return
      clicked = true
      fireEvent.click(radio)
    })
    observer.observe(document.body, { childList: true, subtree: true, attributes: true })
    try {
      render(<QuestionnaireStep sessionToken="first-selection" onCompleteQuestionnaire={vi.fn()} isCompleting={false} completeErrorMessage={null} />)
      await waitFor(() => {
        expect(clicked).toBe(true)
        expect(screen.getByLabelText('Yes')).toHaveProperty('checked', true)
      })
    } finally {
      observer.disconnect()
    }
  })

  it('loads the linked session questions, follows server transitions and allows final submission', async () => {
    get.mockResolvedValue({ data: { currentQuestion: makeQuestion({
      text: 'Which campus feed did you prefer?',
      options: [
        { optionId: 'b', text: 'Feed B', order: 1 },
        { optionId: 'a', text: 'Feed A', order: 0 },
      ],
    }), readyToSubmit: false } })
    put.mockResolvedValueOnce({ data: { currentQuestion: makeQuestion({
      itemId: 'study-text', position: 2, questionType: 'TEXT',
      text: 'Why did you prefer this feed?', required: false, options: [],
    }), readyToSubmit: false } }).mockResolvedValueOnce({ data: { currentQuestion: null, readyToSubmit: true } })
    const complete = vi.fn().mockResolvedValue(true)
    render(<QuestionnaireStep sessionToken="campus-session" studyTitle="Campus study" onCompleteQuestionnaire={complete} isCompleting={false} completeErrorMessage={null} />)

    await screen.findByRole('group', { name: /Which campus feed/ }, { timeout: 3000 })
    expect(get).toHaveBeenCalledWith('/api/participant-session/questionnaire/current', {
      headers: { 'X-Participant-Session-Token': 'campus-session' },
    })
    expect(screen.getByText(/questions for "Campus study"/)).toBeTruthy()
    expect(screen.getAllByRole('radio').map(input => input.parentElement?.textContent)).toEqual(['Feed A', 'Feed B'])
    expect(screen.queryByText(/three sample questions/)).toBeNull()
    await act(async () => { fireEvent.click(screen.getByLabelText('Feed A')) })
    expect(screen.getByLabelText('Feed A')).toHaveProperty('checked', true)
    const next = screen.getByRole('button', { name: 'Next' })
    expect(next).toHaveProperty('disabled', false)
    await act(async () => { fireEvent.click(next) })
    expect(put).toHaveBeenCalledWith('/api/participant-session/questionnaire/answers/item-1', expect.objectContaining({ optionId: 'a' }), {
      headers: { 'X-Participant-Session-Token': 'campus-session', 'Idempotency-Key': expect.any(String) },
    })
    await screen.findByRole('textbox', { name: 'Why did you prefer this feed?' }, { timeout: 3000 })
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Skip' })) })
    await screen.findByRole('button', { name: 'Submit questionnaire' }, { timeout: 3000 })
    expect(put).toHaveBeenLastCalledWith('/api/participant-session/questionnaire/answers/study-text', expect.objectContaining({ unanswered: true }), expect.anything())
    fireEvent.click(screen.getByRole('button', { name: 'Submit questionnaire' }))
    expect(complete).toHaveBeenCalledOnce()
  })

  it('loads a different study session without retaining the old study question', async () => {
    get.mockResolvedValueOnce({ data: { currentQuestion: makeQuestion({ text: 'Study A question' }), readyToSubmit: false } })
      .mockResolvedValueOnce({ data: { currentQuestion: makeQuestion({ itemId: 'b', text: 'Study B question' }), readyToSubmit: false } })
    const props = { onCompleteQuestionnaire: vi.fn(), isCompleting: false, completeErrorMessage: null }
    const { rerender } = render(<QuestionnaireStep key="a" sessionToken="session-a" {...props} />)
    await screen.findByText('Study A question')
    rerender(<QuestionnaireStep key="b" sessionToken="session-b" {...props} />)
    expect(screen.queryByText('Study A question')).toBeNull()
    await screen.findByText('Study B question')
    await waitFor(() => expect(get).toHaveBeenLastCalledWith('/api/participant-session/questionnaire/current', { headers: { 'X-Participant-Session-Token': 'session-b' } }))
  })
})
