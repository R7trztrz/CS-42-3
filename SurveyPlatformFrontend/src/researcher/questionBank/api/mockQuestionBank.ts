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
    questionText: '你每天使用社交媒体的时长大概是多少？',
    required: true,
    options: [
      { id: 'opt-1', optionText: '不到 30 分钟', optionOrder: 0 },
      { id: 'opt-2', optionText: '30 分钟到 2 小时', optionOrder: 1 },
      { id: 'opt-3', optionText: '超过 2 小时', optionOrder: 2 },
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
    questionText: '这条帖子给你的可信度感觉如何？',
    required: true,
    options: [],
    scaleMin: 1,
    scaleMax: 10,
    scaleMinLabel: '完全不可信',
    scaleMaxLabel: '非常可信',
    createdAt: now(),
    updatedAt: now(),
  },
  {
    id: newId(),
    type: 'MULTI_CHOICE',
    questionText: '这条帖子让你产生了哪些情绪？',
    required: false,
    options: [
      { id: 'opt-4', optionText: '好奇', optionOrder: 0 },
      { id: 'opt-5', optionText: '怀疑', optionOrder: 1 },
      { id: 'opt-6', optionText: '愤怒', optionOrder: 2 },
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
    throw new Error('题目内容不能为空。')
  }
  if (
    (values.type === 'SINGLE_CHOICE' || values.type === 'MULTI_CHOICE') &&
    values.options.filter((o) => o.optionText.trim()).length < 2
  ) {
    throw new Error(`${values.type === 'SINGLE_CHOICE' ? '单选题' : '多选题'}至少需要两个选项。`)
  }
  if (values.type === 'SCALE') {
    if (values.scaleMin === null || values.scaleMax === null) {
      throw new Error('打分题必须设置最小值和最大值。')
    }
    if (values.scaleMin >= values.scaleMax) {
      throw new Error('最小值必须小于最大值。')
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
    throw new Error('题目不存在。')
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
    throw new Error('题目不存在。')
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
    throw new Error('题目不存在。')
  }
  store.splice(index, 1)
}

// 供问卷编排器的 mock 模块直接从同一份内存题库中选题，
// 不需要真实后端。
export function mockQuestionBankSnapshot(): QuestionResponse[] {
  return store
}
