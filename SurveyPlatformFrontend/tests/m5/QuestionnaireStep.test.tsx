// @vitest-environment happy-dom

import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'

const { useParticipantQuestionnaireMock } = vi.hoisted(() => ({
  useParticipantQuestionnaireMock: vi.fn(),
}))

vi.mock('../../src/participant/hooks/useParticipantQuestionnaire', () => ({
  useParticipantQuestionnaire: useParticipantQuestionnaireMock,
}))

import QuestionnaireStep from '../../src/participant/components/QuestionnaireStep'
import { makeQuestion } from './fixtures'

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

function baseState(overrides: Record<string, unknown> = {}) {
  return {
    currentQuestion: null,
    readyToSubmit: false,
    isLoading: false,
    isSubmitting: false,
    errorCode: null,
    errorMessage: null,
    submitAnswer: vi.fn().mockResolvedValue(true),
    reload: vi.fn(),
    ...overrides,
  }
}

describe('QuestionnaireStep', () => {
  it('renders SINGLE_CHOICE options and submits the selected optionId', () => {
    const submitAnswer = vi.fn().mockResolvedValue(true)
    useParticipantQuestionnaireMock.mockReturnValue(
      baseState({
        currentQuestion: makeQuestion({ questionType: 'SINGLE_CHOICE' }),
        submitAnswer,
      }),
    )

    render(
      <QuestionnaireStep
        sessionToken="token"
        onCompleteQuestionnaire={vi.fn()}
        isCompleting={false}
        completeErrorMessage={null}
      />,
    )

    fireEvent.click(screen.getByLabelText('Yes'))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(submitAnswer).toHaveBeenCalledWith('item-1', {
      optionId: 'opt-yes',
      optionIds: null,
      scaleValue: null,
      textValue: null,
      unanswered: false,
    })
  })

  it('renders MULTI_CHOICE as checkboxes and submits optionIds', () => {
    const submitAnswer = vi.fn().mockResolvedValue(true)
    useParticipantQuestionnaireMock.mockReturnValue(
      baseState({
        currentQuestion: makeQuestion({ questionType: 'MULTI_CHOICE', required: false }),
        submitAnswer,
      }),
    )

    render(
      <QuestionnaireStep
        sessionToken="token"
        onCompleteQuestionnaire={vi.fn()}
        isCompleting={false}
        completeErrorMessage={null}
      />,
    )

    fireEvent.click(screen.getByLabelText('Yes'))
    fireEvent.click(screen.getByLabelText('No'))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(submitAnswer).toHaveBeenCalledWith('item-1', {
      optionId: null,
      optionIds: ['opt-yes', 'opt-no'],
      scaleValue: null,
      textValue: null,
      unanswered: false,
    })
  })

  it('renders a SCALE question with labelled endpoints and submits the chosen value', () => {
    const submitAnswer = vi.fn().mockResolvedValue(true)
    useParticipantQuestionnaireMock.mockReturnValue(
      baseState({
        currentQuestion: makeQuestion({
          questionType: 'SCALE',
          options: [],
          scaleMin: 1,
          scaleMax: 5,
          scaleMinLabel: 'Not satisfied',
          scaleMaxLabel: 'Very satisfied',
        }),
        submitAnswer,
      }),
    )

    render(
      <QuestionnaireStep
        sessionToken="token"
        onCompleteQuestionnaire={vi.fn()}
        isCompleting={false}
        completeErrorMessage={null}
      />,
    )

    expect(screen.getByText('Not satisfied')).toBeTruthy()
    expect(screen.getByText('Very satisfied')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: '4' }))
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(submitAnswer).toHaveBeenCalledWith('item-1', {
      optionId: null,
      optionIds: null,
      scaleValue: 4,
      textValue: null,
      unanswered: false,
    })
  })

  it('renders a TEXT question as a textarea and submits the typed value', () => {
    const submitAnswer = vi.fn().mockResolvedValue(true)
    useParticipantQuestionnaireMock.mockReturnValue(
      baseState({
        currentQuestion: makeQuestion({ questionType: 'TEXT', options: [], required: false }),
        submitAnswer,
      }),
    )

    render(
      <QuestionnaireStep
        sessionToken="token"
        onCompleteQuestionnaire={vi.fn()}
        isCompleting={false}
        completeErrorMessage={null}
      />,
    )

    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'my answer' } })
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(submitAnswer).toHaveBeenCalledWith('item-1', {
      optionId: null,
      optionIds: null,
      scaleValue: null,
      textValue: 'my answer',
      unanswered: false,
    })
  })

  it('offers Skip only for optional questions, and submits unanswered: true', () => {
    const submitAnswer = vi.fn().mockResolvedValue(true)
    useParticipantQuestionnaireMock.mockReturnValue(
      baseState({
        currentQuestion: makeQuestion({ questionType: 'TEXT', options: [], required: false }),
        submitAnswer,
      }),
    )

    render(
      <QuestionnaireStep
        sessionToken="token"
        onCompleteQuestionnaire={vi.fn()}
        isCompleting={false}
        completeErrorMessage={null}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Skip' }))

    expect(submitAnswer).toHaveBeenCalledWith('item-1', {
      optionId: null,
      optionIds: null,
      scaleValue: null,
      textValue: null,
      unanswered: true,
    })
  })

  it('does not offer Skip for a required question', () => {
    useParticipantQuestionnaireMock.mockReturnValue(
      baseState({
        currentQuestion: makeQuestion({ questionType: 'TEXT', options: [], required: true }),
      }),
    )

    render(
      <QuestionnaireStep
        sessionToken="token"
        onCompleteQuestionnaire={vi.fn()}
        isCompleting={false}
        completeErrorMessage={null}
      />,
    )

    expect(screen.queryByRole('button', { name: 'Skip' })).toBeNull()
  })

  it('disables Next until an answer is present', () => {
    useParticipantQuestionnaireMock.mockReturnValue(
      baseState({ currentQuestion: makeQuestion({ questionType: 'SINGLE_CHOICE' }) }),
    )

    render(
      <QuestionnaireStep
        sessionToken="token"
        onCompleteQuestionnaire={vi.fn()}
        isCompleting={false}
        completeErrorMessage={null}
      />,
    )

    expect((screen.getByRole('button', { name: 'Next' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('shows the submit button once readyToSubmit and currentQuestion are null, and calls onCompleteQuestionnaire', () => {
    const onCompleteQuestionnaire = vi.fn().mockResolvedValue(true)
    useParticipantQuestionnaireMock.mockReturnValue(
      baseState({ currentQuestion: null, readyToSubmit: true }),
    )

    render(
      <QuestionnaireStep
        sessionToken="token"
        onCompleteQuestionnaire={onCompleteQuestionnaire}
        isCompleting={false}
        completeErrorMessage={null}
      />,
    )

    const button = screen.getByRole('button', { name: 'Submit questionnaire' })
    expect((button as HTMLButtonElement).disabled).toBe(false)

    fireEvent.click(button)
    expect(onCompleteQuestionnaire).toHaveBeenCalledTimes(1)
  })

  it('keeps the submit button disabled when not yet readyToSubmit', () => {
    useParticipantQuestionnaireMock.mockReturnValue(
      baseState({ currentQuestion: null, readyToSubmit: false }),
    )

    render(
      <QuestionnaireStep
        sessionToken="token"
        onCompleteQuestionnaire={vi.fn()}
        isCompleting={false}
        completeErrorMessage={null}
      />,
    )

    expect(
      (screen.getByRole('button', { name: 'Submit questionnaire' }) as HTMLButtonElement).disabled,
    ).toBe(true)
  })
})
