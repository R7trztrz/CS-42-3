import { beforeEach, describe, expect, it, vi } from 'vitest'

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('../../src/services/api', () => ({ default: apiMock }))

import {
  listQuestions,
  updateQuestion,
} from '../../src/researcher/questionBank/api/questionBankApi'
import {
  getQuestionnaire,
  saveQuestionnaire,
} from '../../src/researcher/questionnaire/api/questionnaireApi'
import { asApiError } from '../../src/shared/types/apiError'
import { makeQuestionnaire, makeQuestionnaireItem, makeQuestion } from './fixtures'

function apiError(status: number, code: string) {
  return {
    isAxiosError: true,
    response: {
      status,
      data: {
        code,
        message: code,
        timestamp: '2026-10-05T00:00:00Z',
        path: '/api/test',
        details: [],
      },
    },
  }
}

describe('M4 API adapters', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('returns the question page envelope and forwards page, search, and type parameters', async () => {
    const page = {
      content: [],
      page: 2,
      size: 20,
      totalElements: 45,
      totalPages: 3,
    }
    apiMock.get.mockResolvedValueOnce({ data: page })

    await expect(
      listQuestions({ page: 2, size: 20, search: 'branch', type: 'SCALE' }),
    ).resolves.toBe(page)
    expect(apiMock.get).toHaveBeenCalledWith('/api/questions', {
      params: { page: 2, size: 20, search: 'branch', type: 'SCALE' },
    })
  })

  it('retains option IDs and only opts into replacing all identities when none remain', async () => {
    const response = makeQuestion('question-1', 'Choice', 'SINGLE_CHOICE')
    apiMock.put.mockResolvedValue({ data: response })

    await updateQuestion('question-1', {
      type: 'SINGLE_CHOICE',
      questionText: 'Choice',
      required: true,
      options: [
        { optionId: 'option-kept', optionText: 'Kept' },
        { optionId: null, optionText: 'New' },
      ],
      scaleMin: null,
      scaleMax: null,
      scaleMinLabel: null,
      scaleMaxLabel: null,
    })
    expect(apiMock.put.mock.calls[0][1]).toMatchObject({
      replaceAllOptions: false,
      options: [
        { optionId: 'option-kept', optionText: 'Kept' },
        { optionId: null, optionText: 'New' },
      ],
    })

    await updateQuestion('question-1', {
      type: 'TEXT',
      questionText: 'Now text',
      required: false,
      options: [],
      scaleMin: null,
      scaleMax: null,
      scaleMinLabel: null,
      scaleMaxLabel: null,
    })
    expect(apiMock.put.mock.calls[1][1]).toMatchObject({
      replaceAllOptions: true,
      options: [],
    })
  })

  it('converts only QUESTIONNAIRE_NOT_FOUND into an empty draft signal', async () => {
    apiMock.get.mockRejectedValueOnce(apiError(404, 'QUESTIONNAIRE_NOT_FOUND'))
    await expect(getQuestionnaire('study-1')).resolves.toBeNull()

    const missingStudy = apiError(404, 'STUDY_NOT_FOUND')
    apiMock.get.mockRejectedValueOnce(missingStudy)
    await expect(getQuestionnaire('study-1')).rejects.toBe(missingStudy)

    const unauthorized = apiError(401, 'AUTH_UNAUTHORIZED')
    apiMock.get.mockRejectedValueOnce(unauthorized)
    await expect(getQuestionnaire('study-1')).rejects.toBe(unauthorized)
  })

  it('preserves the structured error envelope and sends full-replace questionnaire saves', async () => {
    const conflict = apiError(409, 'QUESTIONNAIRE_VERSION_CONFLICT')
    expect(asApiError(conflict)?.code).toBe('QUESTIONNAIRE_VERSION_CONFLICT')

    const questionnaire = makeQuestionnaire([
      makeQuestionnaireItem('item-1', 0, makeQuestion('question-1', 'Question')),
    ])
    apiMock.put.mockResolvedValueOnce({ data: questionnaire })
    const request = {
      expectedVersion: 0,
      items: [
        { itemId: 'item-1', questionId: 'question-1', branchRules: [] },
      ],
    }
    await expect(saveQuestionnaire('study-1', request)).resolves.toBe(questionnaire)
    expect(apiMock.put).toHaveBeenCalledWith(
      '/api/studies/study-1/questionnaire',
      request,
    )
  })
})
