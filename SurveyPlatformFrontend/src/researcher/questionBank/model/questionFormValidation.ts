import type { QuestionFormValues } from '../../../shared/types/question'

export function validateQuestionForm(values: QuestionFormValues): string[] {
  const errors: string[] = []

  if (!values.questionText.trim()) {
    errors.push('Question text is required.')
  }

  if (values.type === 'SINGLE_CHOICE' || values.type === 'MULTI_CHOICE') {
    if (values.options.length < 2) {
      errors.push('At least two options are required for a choice question.')
    }
    values.options.forEach((option, index) => {
      if (!option.optionText.trim()) {
        errors.push(`Option ${index + 1} must not be blank.`)
      }
    })
  }

  if (values.type === 'SCALE') {
    if (values.scaleMin === null || values.scaleMax === null) {
      errors.push('Scale minimum and maximum are required.')
    } else if (!Number.isInteger(values.scaleMin) || !Number.isInteger(values.scaleMax)) {
      errors.push('Scale minimum and maximum must be whole numbers.')
    } else if (values.scaleMin >= values.scaleMax) {
      errors.push('Scale minimum must be less than scale maximum.')
    }
  }

  return errors
}
