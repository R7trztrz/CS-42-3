import { mockDelay } from '../../../shared/api/mockConfig'
import type {
  QuestionFormValues,
  QuestionResponse,
  QuestionSummaryResponse,
  QuestionType,
} from '../../../shared/types/question'

let nextId = 1
function newId(): string {
  return `mock-question-${nextId++}`
}

function toSummary(question: QuestionResponse): QuestionSummaryResponse {
  return {
    id: question.id,
    type: question.type,
    questionText: question.questionText,
    updatedAt: question.updatedAt,
  }
}

const now = () => new Date().toISOString()

const store: QuestionResponse[] = [
  {
    id: newId(),
    type: 'SINGLE_CHOICE',
    questionText: 'How often do you use social media per day?',
    required: true,
    options: [
      { id: 'opt-1', optionText: 'Less than 30 minutes', optionOrder: 0 },
      { id: 'opt-2', optionText: '30 minutes to 2 hours', optionOrder: 1 },
      { id: 'opt-3', optionText: 'More than 2 hours', optionOrder: 2 },
    ],
    scaleMin: null,
    scaleMax: null,
    scaleMinLabel: null,
    scaleMaxLabel: null,
    createdAt: now(),
    updatedAt: now(),
  },
  {
    id: newId(),
    type: 'SCALE',
    questionText: 'How trustworthy did this post feel?',
    required: true,
    options: [],
    scaleMin: 1,
    scaleMax: 10,
    scaleMinLabel: 'Not trustworthy at all',
    scaleMaxLabel: 'Extremely trustworthy',
    createdAt: now(),
    updatedAt: now(),
  },
  {
    id: newId(),
    type: 'MULTI_CHOICE',
    questionText: 'Which of these emotions did the post evoke?',
    required: false,
    options: [
      { id: 'opt-4', optionText: 'Curiosity', optionOrder: 0 },
      { id: 'opt-5', optionText: 'Skepticism', optionOrder: 1 },
      { id: 'opt-6', optionText: 'Anger', optionOrder: 2 },
    ],
    scaleMin: null,
    scaleMax: null,
    scaleMinLabel: null,
    scaleMaxLabel: null,
    createdAt: now(),
    updatedAt: now(),
  },
]

function validate(values: QuestionFormValues) {
  if (!values.questionText.trim()) {
    throw new Error('Question text is required.')
  }
  if (
    (values.type === 'SINGLE_CHOICE' || values.type === 'MULTI_CHOICE') &&
    values.options.filter((o) => o.optionText.trim()).length < 2
  ) {
    throw new Error(`At least two options are required for ${values.type}.`)
  }
  if (values.type === 'SCALE') {
    if (values.scaleMin === null || values.scaleMax === null) {
      throw new Error('Scale minimum and maximum are required.')
    }
    if (values.scaleMin >= values.scaleMax) {
      throw new Error('Scale minimum must be less than scale maximum.')
    }
  }
}

export async function mockListQuestions(
  type: QuestionType | '',
  keyword: string,
): Promise<QuestionSummaryResponse[]> {
  await mockDelay()
  return store
    .filter((q) => (type ? q.type === type : true))
    .filter((q) =>
      keyword ? q.questionText.toLowerCase().includes(keyword.toLowerCase()) : true,
    )
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
    .map(toSummary)
}

export async function mockGetQuestion(id: string): Promise<QuestionResponse> {
  await mockDelay()
  const question = store.find((q) => q.id === id)
  if (!question) {
    throw new Error('Question not found.')
  }
  return question
}

export async function mockCreateQuestion(
  values: QuestionFormValues,
): Promise<QuestionResponse> {
  await mockDelay()
  validate(values)
  const question: QuestionResponse = {
    id: newId(),
    type: values.type,
    questionText: values.questionText.trim(),
    required: values.required,
    options:
      values.type === 'SINGLE_CHOICE' || values.type === 'MULTI_CHOICE'
        ? values.options.map((o, index) => ({
            id: `opt-${nextId++}`,
            optionText: o.optionText.trim(),
            optionOrder: index,
          }))
        : [],
    scaleMin: values.type === 'SCALE' ? values.scaleMin : null,
    scaleMax: values.type === 'SCALE' ? values.scaleMax : null,
    scaleMinLabel: values.type === 'SCALE' ? values.scaleMinLabel : null,
    scaleMaxLabel: values.type === 'SCALE' ? values.scaleMaxLabel : null,
    createdAt: now(),
    updatedAt: now(),
  }
  store.push(question)
  return question
}

export async function mockUpdateQuestion(
  id: string,
  values: QuestionFormValues,
): Promise<QuestionResponse> {
  await mockDelay()
  validate(values)
  const index = store.findIndex((q) => q.id === id)
  if (index === -1) {
    throw new Error('Question not found.')
  }
  const updated: QuestionResponse = {
    ...store[index],
    type: values.type,
    questionText: values.questionText.trim(),
    required: values.required,
    options:
      values.type === 'SINGLE_CHOICE' || values.type === 'MULTI_CHOICE'
        ? values.options.map((o, orderIndex) => ({
            id: `opt-${nextId++}`,
            optionText: o.optionText.trim(),
            optionOrder: orderIndex,
          }))
        : [],
    scaleMin: values.type === 'SCALE' ? values.scaleMin : null,
    scaleMax: values.type === 'SCALE' ? values.scaleMax : null,
    scaleMinLabel: values.type === 'SCALE' ? values.scaleMinLabel : null,
    scaleMaxLabel: values.type === 'SCALE' ? values.scaleMaxLabel : null,
    updatedAt: now(),
  }
  store[index] = updated
  return updated
}

export async function mockDeleteQuestion(id: string): Promise<void> {
  await mockDelay()
  const index = store.findIndex((q) => q.id === id)
  if (index === -1) {
    throw new Error('Question not found.')
  }
  store.splice(index, 1)
}

// Exposed so the questionnaire editor's mock module can pick items from the
// same in-memory bank without a real backend.
export function mockQuestionBankSnapshot(): QuestionResponse[] {
  return store
}
