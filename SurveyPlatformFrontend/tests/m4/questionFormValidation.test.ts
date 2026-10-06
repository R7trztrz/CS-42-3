import { describe, expect, it } from 'vitest'
import { validateQuestionForm } from '../../src/researcher/questionBank/model/questionFormValidation'
import type { QuestionFormValues } from '../../src/shared/types/question'

function values(overrides: Partial<QuestionFormValues> = {}): QuestionFormValues {
  return {
    type: 'TEXT',
    questionText: 'What happened?',
    required: true,
    options: [],
    scaleMin: null,
    scaleMax: null,
    scaleMinLabel: null,
    scaleMaxLabel: null,
    ...overrides,
  }
}

describe('M4 question form validation', () => {
  it('rejects blank question text', () => {
    expect(validateQuestionForm(values({ questionText: '   ' }))).toContain(
      'Question text is required.',
    )
  })

  it('matches the backend choice option requirements', () => {
    const errors = validateQuestionForm(
      values({
        type: 'SINGLE_CHOICE',
        options: [
          { optionId: 'retained-option', optionText: 'Kept' },
          { optionId: null, optionText: '  ' },
        ],
      }),
    )
    expect(errors).toEqual(['Option 2 must not be blank.'])
    expect(
      validateQuestionForm(
        values({
          type: 'MULTI_CHOICE',
          options: [{ optionId: null, optionText: 'Only one' }],
        }),
      ),
    ).toContain('At least two options are required for a choice question.')
  })

  it('requires ordered integer scale bounds', () => {
    expect(
      validateQuestionForm(values({ type: 'SCALE', scaleMin: null, scaleMax: 5 })),
    ).toContain('Scale minimum and maximum are required.')
    expect(
      validateQuestionForm(values({ type: 'SCALE', scaleMin: 1.5, scaleMax: 5 })),
    ).toContain('Scale minimum and maximum must be whole numbers.')
    expect(
      validateQuestionForm(values({ type: 'SCALE', scaleMin: 5, scaleMax: 5 })),
    ).toContain('Scale minimum must be less than scale maximum.')
    expect(
      validateQuestionForm(values({ type: 'SCALE', scaleMin: 1, scaleMax: 5 })),
    ).toEqual([])
  })
})
